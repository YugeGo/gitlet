package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                Repository.init();
                // TODO: handle the `init` command
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                if(args.length != 2){
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                Repository.add(args[1]);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                // 检查参数数量
                if (args.length != 2) {
                    // 如果只有一个参数 "commit"，则提示输入信息
                    if (args.length == 1) {
                        System.out.println("Please enter a commit message.");
                    } else { // 否则就是参数太多
                        System.out.println("Incorrect operands.");
                    }
                    System.exit(0);
                }

                String message = args[1];
                // 检查 message 是否为空字符串
                if (message.isEmpty()) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }

                Repository.commit(message);
                break;
        }
    }
}
