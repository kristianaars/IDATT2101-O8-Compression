package no.ntnu.idi.krisvaa.idatt2101;

import java.util.Comparator;

public class HuffmanTree {

}

class LeafTreeNode extends TreeNode{
    byte character;

    public LeafTreeNode(byte character, int weight) {
        super(weight, null, null);
        this.character = character;
    }

    @Override
    public String toString() {
        return "LeafTreeNode{" +
                "character=" + character +
                ", weight=" + weight +
                "} " + super.toString();
    }
}

class TreeNode {
    int weight;

    TreeNode parent;
    TreeNode left;
    TreeNode right;

    public TreeNode(int weight, TreeNode left, TreeNode right) {
        this.weight = weight;
        this.left = left;
        this.right = right;

        if(left != null) left.parent = this;
        if(right != null) right.parent = this;
    }



}

class TreeComparator implements Comparator<TreeNode> {

    @Override
    public int compare(TreeNode o1, TreeNode o2) {
        return o1.weight - o2.weight;
    }

}
