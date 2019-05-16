import lombok.extern.slf4j.Slf4j;
import util.FtpClient;
import util.ZipTools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Slf4j
public class FileTrans {
    // 临时存储本次程序运行获取的最大文件传输时间。程序结束前会将其写入配置文件。
    private static String MAX_TRANSPORT_DATE_TEMP = "";

    public void transport() {
        Properties props = getProps();
        File rootDir = new File(props.getProperty("localPath"));
        if (!(rootDir.exists() && rootDir.isDirectory())) {
            log.warn("----本地路径不存在,请检查配置文件是否有误!----");
            return;
        }

        File[] dataDateDirArr = rootDir.listFiles(file -> file.isDirectory() && file.getName().length() == 10);// 列出账期目录

        FtpClient ftpClient = new FtpClient(props.getProperty("ftpIp"),
                Integer.valueOf(props.getProperty("ftpPort")),
                props.getProperty("ftpUser"),
                props.getProperty("ftpPassword"));
        boolean loginSuccess = ftpClient.loginFtp(); // 登录ftp
        if (loginSuccess) {
            for (File dataDateDir : dataDateDirArr) {
                String dataDate = dataDateDir.getName();
                File[] files = dataDateDir.listFiles(file -> {
                    // 获取配置文件中文件最大文件传输时间
                    String maxTransDate = props.getProperty("MAX_TRANS_DATE");
                    maxTransDate = null == maxTransDate || maxTransDate.length() == 0 ? "1900-01-01" : maxTransDate.trim();

                    String fileCreatedDate = getCreateTime(file); // 文件创建时间

                    // 过滤出创建时间大于最大传输时间的excel文件
                    boolean filted = (file.getName().toLowerCase().endsWith(".xls")
                            || file.getName().toLowerCase().endsWith(".xlsx"))
                            && fileCreatedDate.compareTo(maxTransDate) > 0;
                    if (filted) {
                        MAX_TRANSPORT_DATE_TEMP = fileCreatedDate.compareTo(MAX_TRANSPORT_DATE_TEMP) > 0 ? fileCreatedDate : MAX_TRANSPORT_DATE_TEMP;
                    }
                    return filted;
                });
                File zipFile = null;
                try {
                    if (files != null && files.length > 0) {
                        zipFile = new File(dataDateDir.getParentFile().getPath() + "/" + dataDate + ".zip");
                        ZipTools.doCompress(files, zipFile, false);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    log.error("----zip压缩失败!----");
                    continue;
                }
                if (zipFile == null || !zipFile.exists()) {
                    continue;
                }
                String ftpPath = props.getProperty("ftpPath");
                List<String> dirList = ftpClient.getFileNameList(ftpPath);
                if (!dirList.contains(dataDate)) {
                    ftpClient.makeDir(ftpPath + "/" + dataDate);
                }
                ftpClient.changeWorkFile(ftpPath + "/" + dataDate);
                try {
                    ftpClient.uploadFile(zipFile.getName(), new FileInputStream(zipFile));
                    zipFile.delete();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    log.error("----文件" + zipFile.getName() + "不存在!----");
                }
            }
            // 更新配置文件
            if (null != MAX_TRANSPORT_DATE_TEMP && MAX_TRANSPORT_DATE_TEMP.length() > 0) {
                settMaxTransDate(MAX_TRANSPORT_DATE_TEMP);
            }
        } else {
            log.warn("----ftp登录失败！----");
        }
    }

    private static String getCreateTime(File file) {
        Path path = Paths.get(file.getPath());
        BasicFileAttributeView basicView = Files.getFileAttributeView(path, BasicFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS);
        BasicFileAttributes attr;
        try {
            attr = basicView.readAttributes();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String createdDate = sdf.format(new Date(attr.creationTime().toMillis()));
            return createdDate;
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("----" + file.getName() + "获取创建时间失败" + "----");
            return "";
        }
    }

    private static Properties getProps() {
        try {
            Properties pro = new Properties();
            InputStream in = FileTrans.class.getClassLoader().getResourceAsStream("application.properties");
            pro.load(in);
            in.close();
            return pro;
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("----application.properties配置文件解析失败----");
        }
        return null;
    }

    private static boolean settMaxTransDate(String maxTransDate) {
        OutputStream fos = null;
        try {
            String profilepath = FileTrans.class.getClassLoader().getResource("application.properties").getPath();
            Properties props = new Properties();
            props.load(new FileInputStream(profilepath));
            fos = new FileOutputStream(profilepath);
            props.setProperty("MAX_TRANS_DATE", maxTransDate);
            props.store(fos, "Update MAX_TRANS_DATE");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("----属性文件更新失败----");
            return false;
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
