package nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NioServer {

    private final ServerSocketChannel serverChannel = ServerSocketChannel.open();
    private final Selector selector = Selector.open();
    private final ByteBuffer buffer = ByteBuffer.allocate(5);
    private Path serverPath = Paths.get("serverDir");

    public NioServer() throws IOException {
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (serverChannel.isOpen()) {
            selector.select(); // block
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new NioServer();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        while ((read = channel.read(buffer)) > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }
        String command = msg.toString().replaceAll("[\n|\r]", "");
        if (command.equals("ls")) {
            String files = Files.list(serverPath)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.joining(", "));
            files += "\n";
            channel.write(ByteBuffer.wrap(files.getBytes(StandardCharsets.UTF_8)));
        }
        if (command.startsWith("cd ..")) {

                serverPath = serverPath.getParent();
                channel.write(ByteBuffer.wrap(serverPath.toString().getBytes()));

        }
        if(command.startsWith("cd ") && !command.startsWith("cd ..")) {
            String errorMsg = "Directory does not exist";
            Path path = Paths.get(serverPath.toString() + "/" + command.substring(3));
            if(Files.exists(path)){ serverPath = path;
            channel.write(ByteBuffer.wrap(serverPath.toString().getBytes()));
            }else{
                channel.write(ByteBuffer.wrap(errorMsg.getBytes()));
            }


        }
        if(command.startsWith("mkdir ")){
            String errorMsg = "Directory is already exists";
            Path path = Paths.get(serverPath.toString() + "/" + command.substring(6));
            if(!Files.exists(path)) Files.createDirectory(path);
            else channel.write(ByteBuffer.wrap(errorMsg.getBytes()));

        }
        if(command.startsWith("touch ")){
            String errorMsg = "File is already exists";
            Path path = Paths.get(serverPath.toString() + "/" + command.substring(6));
            if(!Files.exists(path)) Files.createFile(path);
            else channel.write(ByteBuffer.wrap(errorMsg.getBytes()));

        }
        if(command.startsWith("cat ")){

            Path path = Paths.get(serverPath.toString() + "/" + command.substring(4));
            String errorMsg = "File does not exist";

            if(Files.exists(path)) {
               List<String> lines = Files.readAllLines(path);
               for (String line : lines) {
                   channel.write(ByteBuffer.wrap(line.getBytes()));
               }
           }else{

               channel.write(ByteBuffer.wrap(errorMsg.getBytes()));
           }
        }


        }


    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }
}
