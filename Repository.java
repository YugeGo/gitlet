package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    //这下面是规定的文件路径
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    //存放所有文件的objects文件夹
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    //存放分支引用的文件夹
    public static final File REFERS_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFERS_DIR, "heads");
    //HEAD指针（指向当前分支）
    public static final File HEAD_FILE = join(GITLET_DIR, "head");
    //暂存区的文件
    public static final File INDEX_FILE = join(GITLET_DIR, "index");

    //这下面是所有的辅助方法
    //辅助方法1，获取当前 HEAD 指针指向的 Commit 对象
    private static Commit getHeadCommit() {
        //去读 .gitlet/HEAD 这个文件的内容
        String path = Utils.readContentsAsString(HEAD_FILE);
        //获取分支文件路径
        String input = path;
        String[] parts = input.split(": ", 2);
        String result = parts[1].trim();
        //读取分支文件内容
        File branchFile = Utils.join(GITLET_DIR, result);
        String headCommitId = Utils.readContentsAsString(branchFile);
        File headCommitFile = Utils.join(OBJECTS_DIR, headCommitId);
        Commit headCommit = Utils.readObject(headCommitFile, Commit.class);
        return headCommit;
    }

    //辅助方法2，获取当前 HEAD 指针指向的 Commit 对象的id
    private static String getHeadCommitId() {
        //去读 .gitlet/HEAD 这个文件的内容
        String path = Utils.readContentsAsString(HEAD_FILE);
        //获取分支文件路径
        String input = path;
        String[] parts = input.split(": ", 2);
        String result = parts[1].trim();
        //读取分支文件内容
        File branchFile = Utils.join(GITLET_DIR, result);
        String headCommitId = Utils.readContentsAsString(branchFile);
        return headCommitId;
    }

    //辅助方法3，持久化commit对象，并返回它的ID
    private static String saveCommit(Commit commit) {
        byte[] commitBytes = Utils.serialize(commit);
        String commitHash = Utils.sha1(commitBytes);
        File commitFile = Utils.join(OBJECTS_DIR, commitHash);
        Utils.writeContents(commitFile, commitBytes);
        return commitHash;
    }

    //辅助方法4，获取当前分支的文件
    private static File getBranchFile() {
        //去读 .gitlet/HEAD 这个文件的内容
        String path = Utils.readContentsAsString(HEAD_FILE);
        //获取分支文件路径
        String[] parts = path.split(": ", 2);
        String result = parts[1].trim();
        //读取分支文件内容
        File branchFile = Utils.join(GITLET_DIR, result);
        return branchFile;
    }

    //辅助方法5，根据ID寻找commit对象
    // 升级版辅助方法：支持 Short ID (前缀匹配)
    private static Commit getCommitById(String commitId) {
        if (commitId == null) {
            return null;
        }

        // 1. 完整长度匹配 (最快)
        if (commitId.length() == Utils.UID_LENGTH) {
            File commitFile = Utils.join(OBJECTS_DIR, commitId);
            if (commitFile.exists()) {
                return Utils.readObject(commitFile, Commit.class);
            }
            return null;
        }

        // 2. 短 ID 匹配 (暴力搜索)
        List<String> allFiles = Utils.plainFilenamesIn(OBJECTS_DIR);
        if (allFiles != null) {
            for (String fileName : allFiles) {
                if (fileName.startsWith(commitId)) {
                    File commitFile = Utils.join(OBJECTS_DIR, fileName);
                    try {
                        return Utils.readObject(commitFile, Commit.class);
                    } catch (IllegalArgumentException e) {
                        return null; // 撞上了 Blob
                    }
                }
            }
        }
        return null;
    }

    // 辅助方法6，根据分支名获取该分支文件
    private static File getBranchFileByName(String branchName) {
        //基础检查
        File targetFile = Utils.join(HEADS_DIR, branchName);
        if (!targetFile.exists()) {
            System.out.println("该分支不存在");
            System.exit(0);
        }
        return targetFile;
    }

    // 辅助方法7，resetToCommit(Commit target),来自checkBranch的安全检查和文件替换逻辑
    private static void resetToCommit(Commit target, String targetCommitId) {

        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        //数据准备，当前工作区的文件遍历，暂存区，目标commit,headCommit的遍历
        //当前工作区文件名集合的获取
        List<String> currentCommitFile = Utils.plainFilenamesIn(CWD);
        Set<String> untrackedFiles = new HashSet<>(currentCommitFile);
        //headCommit文件名集合获取
        Set<String> headCommitFiles = getHeadCommit().getBlobs().keySet();
        //目标commit文件名集合的获取
        Set<String> targetCommitFiles = target.getBlobs().keySet();
        //公式:未追踪文件 = 当前工作区中的文件-headcommit中的文件-暂存区中的文件
        untrackedFiles.removeAll(headCommitFiles);
        untrackedFiles.removeAll(stage.getAddedFiles().keySet());
        //冲突检测
        for (String file : untrackedFiles) {
            if (targetCommitFiles.contains(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        //文件替换
        //把目标分支的文件先全部写入
        for (String fileName : targetCommitFiles) {
            checkoutFile(targetCommitId, fileName);
        }
        //再从工作目录删除目标分支里没有的文件
        for (String files : headCommitFiles) {
            if (!targetCommitFiles.contains(files)) {
                File file = Utils.join(CWD, files);
                Utils.restrictedDelete(file);
            }
        }
    }

    //辅助方法8，getAncestors获取一个分支的所有父节点
    private static Set<String> getAncestors(String commitId) {
        //准备容器
        Set<String> ancestors = new HashSet<>();
        Queue<String> line = new ArrayDeque<>();
        //初始化
        line.add(commitId);
        //开始循环
        while (!line.isEmpty()) {
            String id = line.remove();
            if (ancestors.contains(id)) {
                continue;
            }
            ancestors.add(id);
            Commit commit = getCommitById(id);
            if (commit.getParent() != null) {
                line.add(commit.getParent());
            }
            if ((commit.getSecondParent() != null)) {
                line.add(commit.getSecondParent());
            }
        }
        return ancestors;
    }

    //辅助方法9，撞库
    private static String findSplitPoint(String currentId, String targetId) {
        Set<String> currentAncestors = new HashSet<>(getAncestors(currentId));
        Queue<String> line = new ArrayDeque<>();
        line.add(targetId);
        while (!line.isEmpty()) {
            String checkId = line.remove();
            if (currentAncestors.contains(checkId)) {
                return checkId;
            }
            // ✅ 修正：这里要获取 checkId 的对象，不是 targetId
            Commit commit = getCommitById(checkId);
            if (commit.getParent() != null) {
                line.add(commit.getParent());
            }
            if ((commit.getSecondParent() != null)) {
                line.add(commit.getSecondParent());
            }
        }
        return null;
    }

    //辅助方法10，安全检测（即冲突检测，从7里面摘出来的）
    private static void safetyCheck(Commit target, String targetCommitId) {
        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        //数据准备，当前工作区的文件遍历，暂存区，目标commit,headCommit的遍历
        //当前工作区文件名集合的获取
        List<String> currentCommitFile = Utils.plainFilenamesIn(CWD);
        Set<String> untrackedFiles = new HashSet<>(currentCommitFile);
        //headCommit文件名集合获取
        Set<String> headCommitFiles = getHeadCommit().getBlobs().keySet();
        //目标commit文件名集合的获取
        Set<String> targetCommitFiles = target.getBlobs().keySet();
        //公式:未追踪文件 = 当前工作区中的文件-headcommit中的文件-暂存区中的文件
        untrackedFiles.removeAll(headCommitFiles);
        untrackedFiles.removeAll(stage.getAddedFiles().keySet());
        //冲突检测
        for (String file : untrackedFiles) {
            if (targetCommitFiles.contains(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    //下面是主要方法
    /* TODO: fill in the rest of this class. */
    //init方法
    public static void init() {
        //先检测是否已存在初始化仓库
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        //若不存在初始化，则开始创建文件目录
        GITLET_DIR.mkdirs();
        OBJECTS_DIR.mkdirs();
        HEADS_DIR.mkdirs();
        //加载暂存区
        StagingArea stage = new StagingArea();
        Utils.writeObject(INDEX_FILE, stage);
        //提交一个初始化的commit，并将其序列化得到哈希码,最后存入硬盘
        Commit initialCommit = new Commit(
                "initial commit",
                new Date(0),
                new TreeMap<>(),
                null,
                null
        );
        byte[] commitBytes = Utils.serialize(initialCommit);
        String commitHash = Utils.sha1(commitBytes);
        File commitFile = Utils.join(OBJECTS_DIR, commitHash);
        Utils.writeContents(commitFile, commitBytes);
        //创建master分支,
        File MASTER_FILE = Utils.join(HEADS_DIR, "master");
        //master的内容是最新的commit的哈希码
        Utils.writeContents(MASTER_FILE, commitHash);
        //创建HEAD指针
        Utils.writeContents(HEAD_FILE, "ref: refs/heads/master");
    }

    //add方法
    public static void add(String fileName) {
        File currentFile = Utils.join(CWD, fileName);
        //防御性检查
        if (!currentFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        //读取文件内容并计算 Hash
        byte[] currentFileBytes = Utils.readContents(currentFile);
        String currentFileHash = Utils.sha1(currentFileBytes);
        //准备环境：加载暂存区
        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        //核心比较逻辑
        if (currentFileHash.equals(getHeadCommit().getFileBlobId(fileName))) {
            stage.removeFromAdded(fileName);
            stage.getRemovedFiles().remove(fileName);
        } else {
            // 情况 B: 文件变了 (或者是新文件，headBlobHash 为 null)

            // 持久化 Blob
            // 只有把内容写入 objects，后续 commit 才能引用它
            File blobFile = Utils.join(OBJECTS_DIR, currentFileHash);
            Utils.writeContents(blobFile, currentFileBytes);
            //更新暂存区
            // 调用我们之前写好的 stage.add，它会自动处理 "如果在删除列表则移除" 的逻辑
            stage.add(fileName, currentFileHash);
        }
        Utils.writeObject(INDEX_FILE, stage);
    }

    //commit方法
    public static void commit(String message) {
        //如果 message 是空或者空白字符串，打印 Please enter a commit message. 并退出。
        if (message == null || message.trim().isEmpty()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        //文档规定如果没有暂存变更，不能提交。
        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        if (stage.getAddedFiles().isEmpty() && stage.getRemovedFiles().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        //复制旧快照
        Commit headCommit = getHeadCommit();
        String headCommitId = getHeadCommitId();

        //生成新的快照
        TreeMap<String, String> newBlobs = new TreeMap<>(headCommit.getBlobs());
        //把addedFiles所有的文件加进去
        newBlobs.putAll(stage.getAddedFiles());
        //删除removedFiles中的文件
        for (String fileName : stage.getRemovedFiles()) {
            newBlobs.remove(fileName);
        }
        //封装对象，创建commit对象,并持久化
        Commit newCommit = new Commit(
                message,
                new Date(),
                newBlobs,
                headCommitId,
                null
        );
        String newCommitHash = saveCommit(newCommit);
        //更改最新的分支指向
        File branchFile = getBranchFile();
        Utils.writeContents(branchFile, newCommitHash);
        //清空暂且区
        stage.clear();
        //把暂存区写入硬盘
        Utils.writeObject(INDEX_FILE, stage);
    }

    //log方法
    public static void log() {
        Commit commit = getHeadCommit();
        String commitId = getHeadCommitId();
        while (commit != null) {
            System.out.println("===");
            System.out.println("commit " + commitId);
            // --- 日期格式化开始 ---
            // 格式参考 Spec [cite: 256]
            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
            String dateStr = formatter.format(commit.getTimeStamp());
            System.out.println("Date: " + dateStr);
            // --- 日期格式化结束 ---
            System.out.println(commit.getMessage());
            System.out.println();
            commitId = commit.getParent();
            commit = getCommitById(commitId);

        }
        System.exit(0);
    }
    // global-log: 显示所有提交记录 (无序)
    public static void globalLog() {
        List<String> files = Utils.plainFilenamesIn(OBJECTS_DIR);
        if (files != null) {
            for (String fileName : files) {
                try {
                    File file = Utils.join(OBJECTS_DIR, fileName);
                    // 尝试读取，如果是 Blob 会抛异常被忽略
                    Commit commit = Utils.readObject(file, Commit.class);

                    System.out.println("===");
                    System.out.println("commit " + fileName);
                    SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
                    System.out.println("Date: " + formatter.format(commit.getTimeStamp()));
                    System.out.println(commit.getMessage());
                    System.out.println();
                } catch (IllegalArgumentException e) {
                    // 忽略 Blob 文件
                }
            }
        }
    }

    //checkout [commit id] -- [file name]，把文件恢复到 指定 Commit 里的样子
    public static void checkoutFile(String commitId, String fileName) {
        // 1. 根据 ID 获取 Commit 对象 (记得检查 null)
        Commit commit = getCommitById(commitId);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        // 2. 检查文件是否在 Commit 中
        if (!commit.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobId = commit.getBlobs().get(fileName);
        // 3. 读取 Blob 内容
        File blob = Utils.join(OBJECTS_DIR, blobId);
        byte[] blobContent = Utils.readContents(blob);
        // 4. 写入 CWD
        File workingFile = Utils.join(CWD, fileName);
        Utils.writeContents(workingFile, blobContent);
    }

    //checkout -- [file name],把文件恢复到 HEAD Commit 里的样子
    public static void checkoutHeadFile(String fileName) {
        // 直接调用上面的通用方法，传入 Head Commit ID
        checkoutFile(getHeadCommitId(), fileName);
    }

    //branch(String branchName) 创建一个新的分支
    public static void branch(String branchName) {
        File branchFile = Utils.join(HEADS_DIR, branchName);
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit((0));
        }
        String headCommitId = getHeadCommitId();
        Utils.writeContents(branchFile, headCommitId);
    }

    //rm-branch(String branchName) 删除一个分支标签。
    public static void rmBranch(String branchName) {
        File branchFile = Utils.join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit((0));
        }
        //检查自身：你不能删除你当前正在用的分支！
        if (getBranchFile().getName().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        //
        branchFile.delete();
    }

    // rm [file name]
    public static void rm(String fileName) {
        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        Commit headCommit = getHeadCommit();

        // 1. 获取文件状态
        // 检查是否在暂存区的 "添加列表" 里
        boolean isStaged = stage.getAddedFiles().containsKey(fileName);
        // 检查是否被当前 HEAD Commit 追踪
        // 注意：我们保证了 Commit 的 blobs 永远不为 null (Init 时 new 了 TreeMap)
        boolean isTracked = headCommit.getBlobs().containsKey(fileName);

        // 2. 防御性检查 (Failure Case)
        if (!isStaged && !isTracked) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        // 3. 逻辑 A: 撤销暂存 (Unstage)
        // 如果用户刚 add 了这个文件，rm 会取消这个 add 操作
        if (isStaged) {
            stage.removeFromAdded(fileName);
        }

        // 4. 逻辑 B: 标记删除并物理删除 (Remove & Untrack)
        // 只有被当前 Commit 追踪的文件，才需要“标记为删除”并从磁盘删掉
        if (isTracked) {
            stage.addRemoval(fileName); // 加入 removedFiles 列表

            // 物理删除文件
            File file = Utils.join(CWD, fileName);
            Utils.restrictedDelete(file); // 使用 Utils 提供的安全删除方法
        }

        // 5. 保存暂存区
        Utils.writeObject(INDEX_FILE, stage);
    }

    //status
    public static void status() {
        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        String currentBranchName = getBranchFile().getName();
        //=== Branches ===
        System.out.println("=== Branches ===");
        List<String> branches = Utils.plainFilenamesIn(HEADS_DIR);
        if (branches != null) {
            for (String branchName : branches) {
                if (branchName.equals(currentBranchName)) {
                    System.out.println("*" + branchName);
                } else {
                    System.out.println(branchName);
                }
            }
        }

        System.out.println();
        //=== Staged Files ===
        System.out.println("=== Staged Files ===");
        for (String fileName : stage.getAddedFiles().keySet()) {
            System.out.println(fileName);
        }
        System.out.println();
        //=== Removed Files ===
        System.out.println("=== Removed Files ===");
        for (String fileName : stage.getRemovedFiles()) {
            System.out.println(fileName);
        }
        System.out.println();
        // === Modifications Not Staged For Commit ===
        // 即使没做 Extra Credit，也要把标题打印出来占位
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        // === Untracked Files ===
        System.out.println("=== Untracked Files ===");

        // ⚠️ 关键：最后必须有一个空行
        System.out.println();
    }

    //切换分支 (checkout [branch name])
    public static void checkoutBranch(String branchName) {
        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        //基础检测
        //要切换的分支是否存在
        File targetBranch = Utils.join(HEADS_DIR, branchName);
        if (!targetBranch.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        //要切换的分支是否是当前分支
        if (targetBranch.equals(getBranchFile())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        //安全检查，检测是否有未追踪文件
            /*定义：什么是“未追踪文件 (Untracked File)”？
            既不在当前 Commit 里，也不在暂存区里的文件。
            冲突规则：如果一个文件是 未追踪的，并且它存在于目标分支 (Target Commit) 中
            （意味着切换过去会覆盖它），那么就必须报错。
            报错内容：There is an untracked file in the way; delete it,
            or add and commit it first.*/
        //数据准备，当前工作区的文件遍历，暂存区，目标commit,headCommit的遍历
        //当前工作区文件名集合的获取
        List<String> currentCommitFile = Utils.plainFilenamesIn(CWD);
        Set<String> untrackedFiles = new HashSet<>(currentCommitFile);
        //headCommit文件名集合获取
        Set<String> headCommitFiles = getHeadCommit().getBlobs().keySet();
        //目标commit文件名集合的获取
        String targetCommitId = Utils.readContentsAsString(getBranchFileByName(branchName));
        Commit targetCommit = getCommitById(targetCommitId);
        Set<String> targetCommitFiles = targetCommit.getBlobs().keySet();
        //公式:未追踪文件 = 当前工作区中的文件-headcommit中的文件-暂存区中的文件
        untrackedFiles.removeAll(headCommitFiles);
        untrackedFiles.removeAll(stage.getAddedFiles().keySet());
        //冲突检测
        for (String file : untrackedFiles) {
            if (targetCommitFiles.contains(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        //文件替换
        //把目标分支的文件先全部写入
        for (String fileName : targetCommitFiles) {
            checkoutFile(targetCommitId, fileName);
        }
        //再从工作目录删除目标分支里没有的文件
        for (String files : headCommitFiles) {
            if (!targetCommitFiles.contains(files)) {
                File file = Utils.join(CWD, files);
                Utils.restrictedDelete(file);
            }
        }
        //更新状态
        Utils.writeContents(HEAD_FILE, "ref: refs/heads/" + branchName);
        //清空暂且区
        stage.clear();
        //把暂存区写入硬盘
        Utils.writeObject(INDEX_FILE, stage);

    }

    //reset
    public static void reset(String commitId) {
        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        Commit targetCommit = getCommitById(commitId);
        if (targetCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        //辅助方法7
        resetToCommit(targetCommit, commitId);
        //更改本分支的最新指向
        File currentBranch = Utils.join(HEADS_DIR, getBranchFile().getName());
        Utils.writeContents(currentBranch, commitId);
        //清空暂且区
        stage.clear();
        //把暂存区写入硬盘
        Utils.writeObject(INDEX_FILE, stage);
    }
    // find [message]: 根据消息查找 Commit ID
    public static void find(String message) {
        List<String> files = Utils.plainFilenamesIn(OBJECTS_DIR);
        boolean found = false;

        if (files != null) {
            for (String fileName : files) {
                try {
                    File file = Utils.join(OBJECTS_DIR, fileName);
                    Commit commit = Utils.readObject(file, Commit.class);
                    if (commit.getMessage().equals(message)) {
                        System.out.println(fileName);
                        found = true;
                    }
                } catch (IllegalArgumentException e) {
                    continue; // 忽略 Blob
                }
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    //merge
    public static void merge(String branchName) {
        StagingArea stage = Utils.readObject(INDEX_FILE, StagingArea.class);
        //准备工作
        //1.基本检查:暂存区必须为空，分支必须存在，不能merge自己
        if (!stage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (branchName.equals(getBranchFile().getName())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        File branchFile = Utils.join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        //2.找祖先
        File targetBranch = getBranchFileByName(branchName);
        String targetCommitId = Utils.readContentsAsString(targetBranch);
        String splitCommitId = findSplitPoint(getHeadCommitId(), targetCommitId);
        if ((splitCommitId.equals(targetCommitId))) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitCommitId.equals(getHeadCommitId())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        //3.准备三个Map
        Map<String, String> splitBlobs = getCommitById(splitCommitId).getBlobs();
        Map<String, String> headBlobs = getCommitById(getHeadCommitId()).getBlobs();
        Map<String, String> targetBlobs = getCommitById(targetCommitId).getBlobs();
        //准备全集
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(splitBlobs.keySet());
        allFiles.addAll(headBlobs.keySet());
        allFiles.addAll(targetBlobs.keySet());
        //冲突检测
        safetyCheck(getCommitById(targetCommitId), targetCommitId);
        // 核心逻辑
        for (String fileName : allFiles) {
            String splitHash = splitBlobs.get(fileName);
            String headHash = headBlobs.get(fileName);
            String targetHash = targetBlobs.get(fileName);

            // 1. 默契检测：如果我和别人改得一样 (包括都删了，或者都新增了同样的内容)
            // 利用 Objects.equals 安全处理 null
            if (Objects.equals(headHash, targetHash)) {
                continue; // 保持现状
            }

            // 2. T wins：我没改 (和爷爷一样)，但别人改了
            if (Objects.equals(headHash, splitHash) && !Objects.equals(targetHash, splitHash)) {
                if (targetHash == null) {
                    // 别人删了，我也删
                    rm(fileName);
                } else {
                    // 别人改了/加了，我接受
                    checkoutFile(targetCommitId, fileName);
                    stage.add(fileName, targetHash);
                }
                continue;
            }

            // 3. C wins：别人没改 (和爷爷一样)，但我改了
            if (Objects.equals(targetHash, splitHash) && !Objects.equals(headHash, splitHash)) {
                continue; // 保持现状 (听我的)
            }

            // 4. 冲突！(Conflict)
            // 既不一样，又都动了手脚

            // 构造冲突内容
            String headContent = "";
            if (headHash != null) {
                File headFile = Utils.join(OBJECTS_DIR, headHash);
                headContent = Utils.readContentsAsString(headFile);
            }

            String targetContent = "";
            if (targetHash != null) {
                File targetFile = Utils.join(OBJECTS_DIR, targetHash);
                targetContent = Utils.readContentsAsString(targetFile);
            }

            String conflictContent = "<<<<<<< HEAD\n" + headContent + "=======\n" + targetContent + ">>>>>>>\n";

            File conflictFile = Utils.join(CWD, fileName);
            Utils.writeContents(conflictFile, conflictContent);
            stage.add(fileName, Utils.sha1(conflictContent.getBytes()));

            //  别忘了打印这句
            System.out.println("Encountered a merge conflict.");
        }

        // 5. 最后提交
        // "Merged [target] into [current]."
        String currentBranch = getBranchFile().getName();
        commit("Merged " + branchName + " into " + currentBranch + ".");
    }
    //做完啦！QWQ
}