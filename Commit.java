package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.TreeMap;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private Date timeStamp;
    private String parent;
    private String secondParent;
    private TreeMap<String, String> blobs;

    public Commit(String message, Date timeStamp, TreeMap<String, String> blobs, String parent, String secondParent){
        this.message = message;
        this.timeStamp = timeStamp;
        this.blobs = blobs;
        this.parent = parent;
        this.secondParent = secondParent;
    }

    /* TODO: fill in the rest of this class. */
    // --- Getters ---

    public String getMessage() {
        return message;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public String getParent() {
        return parent;
    }

    public String getSecondParent() {
        return secondParent;
    }

    public TreeMap<String, String> getBlobs() {
        return blobs;
    }

    /**
     * 辅助方法：快速获取某个文件的 Blob Hash
     * 如果文件不存在，返回 null
     */
    public String getFileBlobId(String fileName) {
        return blobs.get(fileName);
    }
}
