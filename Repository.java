package gitlet;

import java.io.File;
import java.util.Date;
import java.util.TreeMap;

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

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    public static final File INDEX_FILE = join(GITLET_DIR, "index");
    /* TODO: fill in the rest of this class. */
    //init命令
    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_DIR.mkdirs();
            OBJECTS_DIR.mkdirs();
            HEADS_DIR.mkdirs();
        }

        Commit initialcommit = new Commit("initial commit", new Date(0), new TreeMap<>(), null, null);

        //持久化commit对象
        //计算哈希码
        String initialcommitID = Utils.sha1(initialcommit.getMessage(),
                String.valueOf(initialcommit.getTimeStamp().getTime()),
                initialcommit.getBlobs().toString(),
                initialcommit.getParent(),
                initialcommit.getSecondParent());
        //将commit对象写入硬盘
        File initialCommit_File = join(OBJECTS_DIR, initialcommitID);
        Utils.writeObject(initialCommit_File, initialcommit);

        //创建master分支
        File MASTER_FILE = join(HEADS_DIR, "master");
        Utils.writeContents(MASTER_FILE, initialcommitID);

        //创建HEAD指针
        Utils.writeContents(HEAD_FILE, "ref: refs/heads/master");
    }
    //获取上一次的commit对象
    public static Commit getHeadCommit(){
        String headContent = Utils.readContentsAsString(HEAD_FILE);
        String[] parts = headContent.split(" "); // 按空格分割
        String branchRefPath = parts[1]; // 取第二部分，得到 "refs/heads/master"
        File branchFile = Utils.join(GITLET_DIR, branchRefPath);
        String headCommitID = Utils.readContentsAsString(branchFile);
        File commitFile = Utils.join(OBJECTS_DIR, headCommitID);
        Commit headCommit = Utils.readObject(commitFile, Commit.class);
        return headCommit;
    }
    //获取当前分支文件
    private static File getCurrentBranchFile(){
        String headContent = Utils.readContentsAsString(HEAD_FILE);
        String[] parts = headContent.split(" "); // 按空格分割
        String branchRefPath = parts[1]; // 取第二部分，得到 "refs/heads/master"
        File branchFile = Utils.join(GITLET_DIR, branchRefPath);
        return branchFile;
    }
    //获取上一次commit对象的ID
    public static String getHeadCommitId(){
        String headContent = Utils.readContentsAsString(HEAD_FILE);
        String[] parts = headContent.split(" "); // 按空格分割
        String branchRefPath = parts[1]; // 取第二部分，得到 "refs/heads/master"
        File branchFile = Utils.join(GITLET_DIR, branchRefPath);
        String headCommitID = Utils.readContentsAsString(branchFile);
        return  headCommitID;
    }
    //add命令
    public static void add(String fileName){
        //检测index文件是否存在
//        StagingArea stagingArea;
//        if(INDEX_FILE.exists()){//若存在就反序列化读出来
//            stagingArea = Utils.readObject(INDEX_FILE, StagingArea.class);
//        }
//        else{//若不存在就建个新的暂存区
//            stagingArea = new StagingArea();
//        }
        StagingArea stagingArea = StagingArea.load();
        //检查要add的文件是否存在于工作目录中
        File fileToAdd = Utils.join(CWD, fileName);
        if(!fileToAdd.exists()){
            System.out.println("File does not exist.");
            System.exit(0);
        }
        else{//获取上一次commit中对应文件的哈希码
            byte[] fileToAddContent = Utils.readContents(fileToAdd);
            String fileToAddContentID = Utils.sha1(fileToAddContent);
            Commit headCommit = getHeadCommit();
            TreeMap<String, String> oldBlobs = headCommit.getBlobs();
            String oldBlobsID = oldBlobs.get(fileName);
            if(fileToAddContentID.equals(oldBlobsID)){//如果新旧ID相同，那么文件没有更改
                 stagingArea.removeFile(fileName);
            }
            else{
                File blobFile = Utils.join(OBJECTS_DIR, fileToAddContentID);
                Utils.writeContents(blobFile, fileToAddContent);
                stagingArea.addFile(fileName, fileToAddContentID);
            }
        }
        Utils.writeObject(INDEX_FILE,stagingArea);
    }

    //持久化辅助函数
    private static String saveCommit(Commit commitToSave){
        //计算哈希码
        String commitID = Utils.sha1(commitToSave.getMessage(),
                String.valueOf(commitToSave.getTimeStamp().getTime()),
                commitToSave.getBlobs().toString(),
                commitToSave.getParent(),
                commitToSave.getSecondParent());
        //写入硬盘
        File COMMIT_TO_SAVE =Utils.join(OBJECTS_DIR, commitID);
        Utils.writeObject(COMMIT_TO_SAVE, commitToSave);
        return commitID;
    }
    //commit命令
    public static void commit(String message){
        //加载暂存区
        StagingArea stagingArea = StagingArea.load();
        //检测暂存区是否为空
        if(stagingArea.isEmpty()){
            System.out.println("没有需要添加的版本");
        }
        //合并算法，将本次要提交的commit与上一次的commit中的blobs合并
        TreeMap<String,String> newBlobs = new TreeMap<>(getHeadCommit().getBlobs());
        newBlobs.putAll(stagingArea.getFilesToAdd());
        //创建一个新的commit对象
        Commit newCommit = new Commit(message, new Date(), newBlobs, getHeadCommitId(), null);
        //持久化
        String newCommitId = saveCommit(newCommit);
        //更新头指针
        File currentBranchFile = getCurrentBranchFile();
        Utils.writeContents(currentBranchFile, newCommitId);
        //清空暂存区
        stagingArea.clear();
    }
}
