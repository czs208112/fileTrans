package util;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ftp客户端
 *
 * @author Administrator
 */
public class FtpClient {
    private org.apache.commons.net.ftp.FTPClient ftpClient;
    private String serverIp;
    private int port = 21;
    private String userName;
    private String password;

    public FtpClient() {
    }

    public org.apache.commons.net.ftp.FTPClient getOriginFtpClient() {
        return ftpClient;
    }

    //创建ftp访问对象
    public FtpClient(String serverIp, int port, String userName, String password) {
        this.serverIp = serverIp;
        this.userName = userName;
        this.password = password;
        this.port = port;
    }

    //登录ftp
    public boolean loginFtp() {
        try {
            this.ftpClient = new org.apache.commons.net.ftp.FTPClient();
            ftpClient.connect(this.serverIp, this.port);
            ftpClient.login(this.userName, this.password);
            int replayCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replayCode)) {
                ftpClient.disconnect();
                return false;
            }
            ftpClient.setBufferSize(2048);
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    //网络文件下载
    public InputStream downLoadFileForNet(String filePath) {
        try {
            InputStream in = ftpClient.retrieveFileStream(filePath);
            return in;
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> downLoadReadFileContent(String filePath) {
        BufferedReader reader = null;
        InputStreamReader streamReader = null;
        InputStream in = null;
        List<String> result = new ArrayList<String>();
        try {
            in = ftpClient.retrieveFileStream(filePath);
            streamReader = new InputStreamReader(in);
            reader = new BufferedReader(streamReader);
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line == null || line.trim().length() == 0) continue;
                result.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
            try {
                streamReader.close();
            } catch (Exception e) {
            }
            try {
                in.close();
            } catch (Exception e) {
            }
        }
        return result;
    }

    public boolean downloadFile(String suffix, String localpath) {
        boolean flag = false;
        OutputStream os = null;
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles();
            for (FTPFile file : ftpFiles) {
                if (suffix == null || file.getName().endsWith(suffix)) { //限制后缀
                    File localFile = new File(localpath + "/" + file.getName());
                    os = new FileOutputStream(localFile);
                    ftpClient.retrieveFile(new String(file.getName().getBytes("UTF-8"), "ISO-8859-1"), os);
                    os.close();
                }
            }
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
        return flag;
    }

    //上传文件
    public boolean uploadFile(String fileName, InputStream in) {
        try {
            ftpClient.storeFile(fileName, in);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }
        }
    }

    //定位到操作目录
    public boolean changeWorkFile(String filePath) {
        try {
            ftpClient.changeWorkingDirectory(filePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //创建文件夹
    public boolean makeDir(String dirPath) {
        String[] dirs = dirPath.split("/");
        StringBuilder sb = new StringBuilder();
        for (String d : dirs) {
            try {
                sb.append(d + "/");
                ftpClient.makeDirectory(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        return true;
    }

    public void deleteFileFromFtp(String filePathName) {
        try {
            ftpClient.deleteFile(filePathName);
        } catch (Exception e) {
        }
    }

    //关闭ftp
    public void closeFtp() {
        try {
            if (this.ftpClient != null && this.ftpClient.isConnected()) {
                this.ftpClient.logout();
                this.ftpClient.disconnect();
            }
        } catch (Exception e) {
        }
    }

    /**
     * 返回FTP目录下的文件列表
     *
     * @param ftpDirectory
     * @return
     */
    public List<String> getFileNameList(String ftpDirectory) {
        List<String> list = new ArrayList<String>();
        try {
            FTPFile[] files = ftpClient.listFiles(ftpDirectory);
            for (FTPFile file : files) {
                list.add(file.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
