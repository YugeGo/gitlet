package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.TreeMap;

public class StagingArea implements Serializable {
    //待添加\修改的文件
    TreeMap<String,String> fileToAdd;

    //构造方法
    public StagingArea(){
        this.fileToAdd = new TreeMap<>();
    }
    //添加或更新一个文件
    public void addFile(String fileName, String blobID){
        this.fileToAdd.put(fileName, blobID);
    }
    //删除文件
    public void removeFile(String fileName){
        this.fileToAdd.remove(fileName);
    }
    //检查暂存区是否为空
    public boolean isEmpty(){
        return this.fileToAdd.isEmpty();
    }
    //加载暂存区
    public static StagingArea load(){
        File INDEX_FILE = Utils.join(Repository.GITLET_DIR, "index");
        StagingArea stagingArea;
        if(INDEX_FILE.exists()){//若存在就反序列化读出来
            stagingArea = Utils.readObject(INDEX_FILE, StagingArea.class);
        }
        else{//若不存在就建个新的暂存区
            stagingArea = new StagingArea();
        }
        return stagingArea;
    }

    //获取暂存区中的blobs
    public TreeMap<String, String> getFilesToAdd(){
        return this.fileToAdd;
    }
    //清空暂存区
    public void clear() {
        this.fileToAdd.clear(); // TreeMap 自带的清空方法
    }
}
