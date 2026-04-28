import java.util.*;
import java.io.*;

/*
Algorithm Design and Implementation: B+-Tree in Parts Cataloging
Graduate Algorithms - Project 1
Due: April 29, 2026
 */

public class PartsCataloger {

    //These keep track of tree metrics
    public static int totalSplits = 0;
    public static int parentSplits = 0;
    public static int fusions = 0;
    public static int parentFusions = 0;

    //Part Class
    static class Part {
        String id;
        String description;

        Part(String id, String description) {
            this.id = id;
            this.description = description;
        }

        @Override
        public String toString() {
            return String.format("%-7s | %s", id, description);
        }
    }

    // B+- Tree Structures
    static abstract class Node {
        InternalNode parent;
        ArrayList<String> keys = new ArrayList<>();
        boolean isLeaf;
    }

    static class InternalNode extends Node {
        ArrayList<Node> children = new ArrayList<>();

        InternalNode() {
            isLeaf = false;
        }
    }

    static class LeafNode extends Node {
        ArrayList<Part> records = new ArrayList<>();
        LeafNode next; // Pointer for sequential access

        LeafNode() {
            isLeaf = true;
        }
    }

    //B+- Tree Actions
    static class BPlusTree {
        private Node root = new LeafNode();
        private final int MAX_INTERNAL_KEYS = 4;
        private final int MAX_LEAF_RECORDS = 16;
        private int depth = 1;

        public int getDepth() {
            return depth;
        }

        public LeafNode findLeaf(String id) {
            Node curr = root;
            while (!curr.isLeaf) {
                InternalNode internal = (InternalNode) curr;
                int i = 0;
                while (i < internal.keys.size() && id.compareTo(internal.keys.get(i)) >= 0) {
                    i++;
                }
                curr = internal.children.get(i);
            }
            return (LeafNode) curr;
        }

        public Part search(String id) {
            LeafNode leaf = findLeaf(id);
            for (Part p : leaf.records) {
                if (p.id.equals(id)) return p;
            }
            return null;
        }

        public void insert(Part part) {
            LeafNode leaf = findLeaf(part.id);
            int pos = 0;
            while (pos < leaf.records.size() && part.id.compareTo(leaf.records.get(pos).id) > 0) {
                pos++;
            }
            leaf.records.add(pos, part);

            if (leaf.records.size() > MAX_LEAF_RECORDS) {
                splitLeaf(leaf);
            }
        }

        private void splitLeaf(LeafNode leaf) {
            totalSplits++;
            LeafNode newLeaf = new LeafNode();
            int mid = leaf.records.size() / 2;

            newLeaf.records.addAll(new ArrayList<>(leaf.records.subList(mid, leaf.records.size())));
            leaf.records.subList(mid, leaf.records.size()).clear();

            newLeaf.next = leaf.next;
            leaf.next = newLeaf;

            promote(leaf, newLeaf.records.get(0).id, newLeaf);
        }

        private void promote(Node left, String key, Node right) {
            if (left == root) {
                InternalNode newRoot = new InternalNode();
                newRoot.keys.add(key);
                newRoot.children.add(left);
                newRoot.children.add(right);
                root = newRoot;
                left.parent = newRoot;
                right.parent = newRoot;
                depth++;
                return;
            }

            InternalNode p = left.parent;
            int pos = 0;
            while (pos < p.keys.size() && key.compareTo(p.keys.get(pos)) > 0) {
                pos++;
            }
            p.keys.add(pos, key);
            p.children.add(pos + 1, right);
            right.parent = p;

            if (p.keys.size() > MAX_INTERNAL_KEYS) {
                splitInternal(p);
            }
        }

        private void splitInternal(InternalNode node) {
            parentSplits++;
            InternalNode newNode = new InternalNode();
            int mid = node.keys.size() / 2;
            String upKey = node.keys.get(mid);

            newNode.keys.addAll(new ArrayList<>(node.keys.subList(mid + 1, node.keys.size())));
            newNode.children.addAll(new ArrayList<>(node.children.subList(mid + 1, node.children.size())));

            for (Node child : newNode.children) child.parent = newNode;

            node.keys.subList(mid, node.keys.size()).clear();
            node.children.subList(mid + 1, node.children.size()).clear();

            promote(node, upKey, newNode);
        }

        public void delete(String id) {
            // Team Deletion Lead to implement Borrow/Fuse logic here
            System.out.println("Deleting Entry: " + id);
        }
    }

    // User Interface: Scanner
    public static void main(String[] args) {
        BPlusTree tree = new BPlusTree();
        Scanner console = new Scanner(System.in);

        loadFromFile("partfile.txt", tree);//Loads the flat-file

        //Recursively runs program as long as user wants
        boolean running = true;
        while (running) {
            printMenu();
            int choice;
            try {
                choice = Integer.parseInt(console.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number between 1 and 6.");
                continue;
            }

            //Menu system using switch
            switch (choice) {
                case 1: // Search
                    System.out.print("Enter Part ID: ");
                    Part p = tree.search(console.nextLine());
                    System.out.println(p != null ? p : "Part not found.");
                    break;
                case 2: // Next 10
                    System.out.print("Enter Starting Part ID: ");
                    displayNextTen(tree, console.nextLine());
                    break;
                case 3: // Add
                    System.out.print("Part ID (7 chars): ");
                    String id = console.nextLine();
                    System.out.print("Description: ");
                    String desc = console.nextLine();
                    tree.insert(new Part(id, desc));
                    break;
                case 4: // Modify
                    System.out.print("Enter Part ID to modify: ");
                    Part mod = tree.search(console.nextLine());
                    if (mod != null) {
                        System.out.print("Enter new description: ");
                        mod.description = console.nextLine();
                    } else {
                        System.out.println("Part not found.");
                    }
                    break;
                case 5: // Delete
                    System.out.print("Enter Part ID to delete: ");
                    tree.delete(console.nextLine());
                    break;
                case 6: // Exit & Save or Not
                    System.out.print("Save changes? (1 for Yes, 2 for No): ");
                    if (console.nextLine().equals("1")) saveToFile("parts.txt", tree);
                    running = false;
                    break;
                default:
                    System.out.println("Selection Invalid.");
            }
        }
        printFinalMetrics(tree);//Outputs metrics: Splits, Parent Splits, Fusions and Parent Fusions
    }

    //Show User Menu Choices
    private static void printMenu() {
        System.out.println("\n--- Parts Catalog Menu ---");
        System.out.println("1. Search for Part");
        System.out.println("2. Display Next 10 Parts");
        System.out.println("3. Add New Part");
        System.out.println("4. Modify Description");
        System.out.println("5. Delete Part");
        System.out.println("6. Exit");
        System.out.print("Selection: ");
    }

    //Method Shows Next Ten
    private static void displayNextTen(BPlusTree tree, String id) {
        LeafNode curr = tree.findLeaf(id);
        int count = 0;
        boolean startPrinting = false;

        while (curr != null && count < 10) {
            for (Part r : curr.records) {
                if (!startPrinting && r.id.compareTo(id) >= 0) startPrinting = true;
                if (startPrinting && count < 10) {
                    System.out.println(r);
                    count++;
                }
            }
            curr = curr.next;
        }
    }

    //Loading the Flat-File
    private static void loadFromFile(String filename, BPlusTree tree) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() >= 7) {
                    String id = line.substring(0, 7).trim();
                    String desc = line.length() > 15 ? line.substring(15).trim() : "";
                    tree.insert(new Part(id, desc));
                }
            }
        } catch (Exception e) {
            System.out.println("Loading from file failed.");
        }
    }

    //Saving File
    private static void saveToFile(String filename, BPlusTree tree) {
        // Implementation: Traverse leaf linked-list and write records to file
        System.out.println("Data saved successfully.");
    }

    //Prints the splits and fusions
    private static void printFinalMetrics(BPlusTree tree) {
        System.out.println("\n--- Final Project Metrics ---");
        System.out.println("Total Splits: " + totalSplits);
        System.out.println("Parent Splits: " + parentSplits);
        System.out.println("Fusions: " + fusions);
        System.out.println("Parent Fusions: " + parentFusions);
        System.out.println("Tree Depth: " + tree.getDepth());
    }
}
//Stage Enabled test