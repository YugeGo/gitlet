package gitlet;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 暂存区 (Index)
 * 负责追踪文件的添加 (Add) 和移除 (Remove) 状态。
 */
public class StagingArea implements Serializable {

    /**
     * 存储 "准备提交的修改"。
     * Key: 文件名
     * Value: Blob 的 SHA-1 哈希值
     * 必须用 TreeMap 以保证序列化顺序确定性 [cite: 1015]
     */
    private TreeMap<String, String> addedFiles;

    /**
     * 存储 "准备删除的文件"。
     * 仅仅存储文件名。
     * 必须用 TreeSet 以保证序列化顺序确定性 [cite: 1015]
     */
    private TreeSet<String> removedFiles;

    /** 构造函数：初始化两个集合 */
    public StagingArea() {
        this.addedFiles = new TreeMap<>();
        this.removedFiles = new TreeSet<>();
    }

    /**
     * 核心方法 1: add
     * 将文件加入暂存区 (准备添加/更新)
     * 逻辑:
     * 1. 如果该文件之前被标记为 "移除"，则取消移除标记 。
     * 2. 将文件和对应的 Blob 哈希存入 addedFiles (如果已存在则覆盖) [cite: 190]。
     */
    public void add(String fileName, String blobHash) {
        if (removedFiles.contains(fileName)) {
            removedFiles.remove(fileName);
        }
        addedFiles.put(fileName, blobHash);
    }

    /**
     * 核心方法 2: addRemoval (对应 gitlet rm)
     * 将文件标记为 "准备删除"
     * 逻辑:
     * 1. 如果文件在 addedFiles 里 (即刚 add 过)，直接移除它 (Unstage) 。
     * 2. 将文件名加入 removedFiles [cite: 243]。
     */
    public void addRemoval(String fileName) {
        if (addedFiles.containsKey(fileName)) {
            addedFiles.remove(fileName);
        }
        removedFiles.add(fileName);
    }

    /**
     * 辅助方法: 强制从 added 区域移除
     * (用于 Repository.add 发现文件内容与 Head 一致时调用)
     */
    public void removeFromAdded(String fileName) {
        addedFiles.remove(fileName);
    }

    /**
     * 检查暂存区是否是干净的 (给 commit 命令用) [cite: 231]
     */
    public boolean isEmpty() {
        return addedFiles.isEmpty() && removedFiles.isEmpty();
    }

    /**
     * 提交后清空暂存区 [cite: 213]
     */
    public void clear() {
        addedFiles.clear();
        removedFiles.clear();
    }

    // --- Getters ---

    public TreeMap<String, String> getAddedFiles() {
        return addedFiles;
    }

    public TreeSet<String> getRemovedFiles() {
        return removedFiles;
    }
}
