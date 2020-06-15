package org.janelia.saalfeldlab.n5;

import se.sawano.java.text.AlphanumericComparator;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

public class N5DatasetDiscoverer {

    private final Comparator<? super String> comparator;

    /**
     * Creates an N5 discoverer with alphanumeric sorting order of groups/datasets (such as, s9 goes before s10).
     */
    public N5DatasetDiscoverer() {

        this(Optional.of(new AlphanumericComparator(Collator.getInstance())));
    }

    /**
     * Creates an N5 discoverer.
     *
     * If the optional parameter {@code comparator} is specified, the groups and datasets
     * will be listed in the order determined by this comparator.
     *
     * @param comparator
     */
    public N5DatasetDiscoverer(final Optional<Comparator<? super String>> comparator) {

        this.comparator = comparator.orElseGet(null);
    }

    public N5TreeNode discover(final N5Reader n5) throws IOException {

        final N5TreeNode root = new N5TreeNode("/");
        discover(n5, root);
        trim(root);
        if (comparator != null)
            sort(root, comparator);
        return root;
    }

    public static DefaultMutableTreeNode toJTreeNode(final N5TreeNode n5Node)
    {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(n5Node);
        for (final N5TreeNode n5ChildNode : n5Node.children)
            node.add(toJTreeNode(n5ChildNode));
        return node;
    }

    private static void discover(final N5Reader n5, final N5TreeNode node) throws IOException {

        if (n5.datasetExists(node.path)) {
            node.isDataset = true;
        } else {
            for (final String childGroup : n5.list(node.path)) {
                final String childPath = Paths.get(node.path, childGroup).toString();
                final N5TreeNode childNode = new N5TreeNode(childPath);
                node.children.add(childNode);
                discover(n5, childNode);
            }
        }
    }

    /**
     * Removes branches of the N5 container tree that do not contain datasets.
     *
     * @param node
     * @return
     *      {@code true} if the branch contains a dataset, {@code false} otherwise
     */
    private static boolean trim(final N5TreeNode node)
    {
        if (node.children.isEmpty())
            return node.isDataset;

        boolean ret = false;
        for (final Iterator<N5TreeNode> it = node.children.iterator(); it.hasNext();)
        {
            final N5TreeNode childNode = it.next();
            if (!trim(childNode))
                it.remove();
            else
                ret = true;
        }
        return ret;
    }

    private static void sort(final N5TreeNode node, final Comparator<? super String> comparator)
    {
        node.children.sort(Comparator.comparing(N5TreeNode::toString, comparator));
        for (final N5TreeNode childNode : node.children)
            sort(childNode, comparator);
    }
}
