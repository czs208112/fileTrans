package util;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipTools {
    private ZipTools() {
    }

    public static void doCompress(String srcFile, String zipFile, boolean haveDirEntry) throws IOException {
        doCompress(new File(srcFile), new File(zipFile), haveDirEntry);
    }

    /**
     * 文件压缩
     *
     * @param srcFile      目录或者单个文件
     * @param zipFile      压缩后的ZIP文件
     * @param haveDirEntry 压缩文件是否创建目录
     */
    public static void doCompress(File srcFile, File zipFile, boolean haveDirEntry) throws IOException {
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(zipFile));
            doCompress(srcFile, out, haveDirEntry);
        } catch (Exception e) {
            throw e;
        } finally {
            out.close();//关闭资源
        }
    }

    public static void doCompress(String filelName, ZipOutputStream out, boolean haveDirEntry) throws IOException {
        doCompress(new File(filelName), out, haveDirEntry);
    }

    public static void doCompress(File file, ZipOutputStream out, boolean haveDirEntry) throws IOException {
        doCompress(file, out, "", haveDirEntry);
    }

    public static void doCompress(File inFile, ZipOutputStream out, String dir, boolean haveDirEntry) throws IOException {
        if (inFile.isDirectory()) {
            File[] files = inFile.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    String name = inFile.getName();
                    if (!"".equals(dir)) {
                        name = dir + "/" + name;
                    }
                    ZipTools.doCompress(file, out, name, haveDirEntry);
                }
            }
        } else {
            ZipTools.doZip(inFile, out, dir, haveDirEntry);
        }
    }

    public static void doCompress(File[] files, File zipFile, boolean haveDirEntry) throws IOException {
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(zipFile));
            if (files != null && files.length > 0) {
                for (File file : files) {
                    ZipTools.doCompress(file, out, zipFile.getPath(), haveDirEntry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    public static void doCompress(File[] srcFiles, File zipFile) throws IOException {
        // 判断压缩后的文件存在不，不存在则创建
        if (!zipFile.exists()) {
            try {
                zipFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 创建 FileOutputStream 对象
        FileOutputStream fileOutputStream = null;
        // 创建 ZipOutputStream
        ZipOutputStream zipOutputStream = null;
        // 创建 FileInputStream 对象
        FileInputStream fileInputStream = null;

        // 实例化 FileOutputStream 对象
        fileOutputStream = new FileOutputStream(zipFile);
        // 实例化 ZipOutputStream 对象
        zipOutputStream = new ZipOutputStream(fileOutputStream);
        // 创建 ZipEntry 对象
        ZipEntry zipEntry = null;
        // 遍历源文件数组
        for (int i = 0; i < srcFiles.length; i++) {
            // 将源文件数组中的当前文件读入 FileInputStream 流中
            fileInputStream = new FileInputStream(srcFiles[i]);
            // 实例化 ZipEntry 对象，源文件数组中的当前文件
            zipEntry = new ZipEntry(srcFiles[i].getName());
            zipOutputStream.putNextEntry(zipEntry);
            // 该变量记录每次真正读的字节个数
            int len;
            // 定义每次读取的字节数组
            byte[] buffer = new byte[1024];
            while ((len = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, len);
            }
        }
        zipOutputStream.closeEntry();
        zipOutputStream.close();
        fileInputStream.close();
        fileOutputStream.close();

    }

    public static void doZip(File inFile, ZipOutputStream out, String dir, boolean haveDirEntry) throws IOException {
        String entryName = null;
        if (haveDirEntry && !"".equals(dir)) {
            entryName = dir + "/" + inFile.getName();
        } else {
            entryName = inFile.getName();
        }

        String path = inFile.getPath();
        File file = new File(path);
        if (!file.exists()) return;
        ZipEntry entry = new ZipEntry(entryName);
        out.putNextEntry(entry);

        int len = 0;
        byte[] buffer = new byte[1024];
        FileInputStream fis = new FileInputStream(inFile);
        while ((len = fis.read(buffer)) > 0) {
            out.write(buffer, 0, len);
            out.flush();
        }
        out.closeEntry();
        fis.close();
    }


    public static void unzip(String dir, File source) {
        if (source.exists()) {
            ZipInputStream zis = null;
            BufferedOutputStream bos = null;
            try {
                zis = new ZipInputStream(new FileInputStream(source));
                ZipEntry entry = null;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        File target = new File(dir, entry.getName());
                        bos = new BufferedOutputStream(new FileOutputStream(target));
                        int read = 0;
                        byte[] buffer = new byte[1024 * 10];
                        while ((read = zis.read(buffer, 0, buffer.length)) != -1) {
                            bos.write(buffer, 0, read);
                        }
                        bos.flush();
                        ZipTools.close(bos);
                    }
                }
                zis.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                ZipTools.close(zis, bos);
                source.delete();
            }
        }
    }

    public final static boolean unZipFile(String dir, File f) {
        try {
            ZipFile zipFile = new ZipFile(f);
            File dirFile = new File(dir);
            if (!dirFile.exists()) dirFile.mkdirs();
            Enumeration e = zipFile.entries();
            while (e.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) e.nextElement();
                if (zipEntry.isDirectory()) {
                    continue;
                }
                String name = zipEntry.getName();
                name.replaceAll("\\\\", "/");
                if (name.indexOf("/") != -1)
                    name = name.substring(name.lastIndexOf("/") + 1);
                //String fname = name.substring(0,name.lastIndexOf("."));
                File newFile = new File(dir, name);
                //File newFile = new File(dir, name);
                if (!newFile.exists()) {
                    newFile.createNewFile();
                }
                //replaceR2(zipFile.getInputStream(zipEntry),new FileOutputStream(newFile));
                // copyFile(zipFile.getInputStream(zipEntry),new FileOutputStream(newFile));
                //    FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry),newFile);
                copyInputStreamToFile(zipFile.getInputStream(zipEntry), newFile);
                //appendContentToFile(newFile,"\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            f.delete();
        }
        return true;
    }

    private static void copyInputStreamToFile(InputStream inputStream, File file) {
        OutputStream out = null;
        try {
            byte[] buf = new byte[1024];
            out = new FileOutputStream(file);
            // 开始读取数据
            int len = -1;// 每次读取到的数据的长度
            while ((len = inputStream.read(buf)) != -1) {// len值为-1时，表示没有数据了
                out.write(buf, 0, len);
            }
        } catch (Exception e) {

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }


    public final static void copyFile(InputStream in, OutputStream out) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            writer = new BufferedWriter(new OutputStreamWriter(out));
            String row = null;
            while ((row = reader.readLine()) != null) {
                //writer.write(xxx2utf(xxx2utf(row)));
                String temp = row + "\r\n";

                writer.write(row + "\r\n");
            }
        } catch (Exception e) {

        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (Exception e) {

            }
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
            }
        }
    }

    private static void close(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
