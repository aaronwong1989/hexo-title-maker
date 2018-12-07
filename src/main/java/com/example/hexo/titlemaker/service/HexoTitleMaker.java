package com.example.hexo.titlemaker.service;

import com.example.hexo.titlemaker.domain.Blog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.zip.CRC32;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 为一个目录下的所有markdown文件添加 hexo title<br> 替换文件中的链接为本地链接
 *
 * @author Aaron
 */
@Component
@Order(2)
public class HexoTitleMaker implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(HexoTitleMaker.class);

  private static final String SUB_LIST_PRE = "  - ";
  private static final String LN = "\n";
  private static final String BORDER = "---\n";
  private static final String MD1 = ".md";
  private static final String MD2 = ".MD";
  private static final String FILE_NAME = "{hexo.file.name}";
  private static final String ABBR_LINK = "{hexo.abbrlink}";
  private static final String UTF_8 = "UTF-8";
  private static final String SM = "> ";
  private static final String SRC = "src";
  private static final String TARGET = "target";
  private static final String READ_MORE = "\n<!-- more -->\n";
  private static final int LINES_IN_HOME_PAGE = 12;


  @Resource
  Blog blog;

  private File outputDir;

  @PostConstruct
  public void init() {
    outputDir = new File(blog.getMdPath() + File.separator + "out" + File.separator);
    if (!outputDir.exists()) {
      log.info("创建输出文件夹{} : {}", outputDir.toString(), outputDir.mkdirs());
    }
  }
  @Override
  public void run(String... args) throws Exception {
    {
      log.info(blog.toString());
      File dir = new File(blog.getMdPath());
      if (dir.exists()) {
        StringBuilder hexoTitleBd = new StringBuilder(512);
        //start yaml
        hexoTitleBd.append(BORDER);
        // title
        hexoTitleBd.append(Blog.TITLE).append(blog.getTitle()).append(SM + FILE_NAME + LN);
        // tags
        hexoTitleBd.append(Blog.TAGS).append(LN);
        for (String tag : blog.getTags()) {
          hexoTitleBd.append(SUB_LIST_PRE).append(tag).append(LN);
        }
        // categories
        hexoTitleBd.append(Blog.CATEGORIES).append(LN);
        for (String cat : blog.getCategories()) {
          hexoTitleBd.append(SUB_LIST_PRE).append(cat).append(LN);
        }
        // abbrlink
        hexoTitleBd.append(Blog.ABBRLINK).append(ABBR_LINK + LN);
        // top
        hexoTitleBd.append(Blog.TOP).append(blog.getTop()).append(LN);
        //end yaml
        hexoTitleBd.append(BORDER);

        // 根据文章内容获取CRC
        final Map<String, String> nameCrcMap = new HashMap<>();
        Arrays.stream(Objects.requireNonNull(
            dir.listFiles(
                f -> (f.isFile() || f.getName().endsWith(MD1) || f.getName().endsWith(MD2)))))
            .forEach(
                f -> {
                  String fileTitle = f.getName().replace(MD1, "").replace(MD2, "");
                  byte[] bts = new byte[(int) f.length()];
                  try (InputStream in = new FileInputStream(f)) {
                    IOUtils.read(in, bts);
                    // 读取每个文件获取 文件名:CRC 信息存储到map中
                    CRC32 crc32 = new CRC32();
                    crc32.update(bts);
                    nameCrcMap
                        .put(URLEncoder.encode(fileTitle, UTF_8), Long.toHexString(crc32.getValue()));
                    crc32 = null;
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
            );

        // 重写每一个markdown文件
        Arrays.stream(Objects.requireNonNull(
            dir.listFiles(
                f -> (f.isFile() || f.getName().endsWith(MD1) || f.getName().endsWith(MD2)))))
            .forEach(f -> {
              File outFile = new File(outputDir.getAbsolutePath() + File.separator + f.getName());
              if (!outFile.exists()) {
                try {
                  log.info("创建文件{} : {}", outFile.toString(), outFile.createNewFile());
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
              // The try-with-resources Statement
              try (InputStream in = new FileInputStream(f);
                  Writer writer = new FileWriterWithEncoding(outFile, UTF_8, false);) {
                // yamlTitle
                String yamlTitle = hexoTitleBd.toString();
                String filename = f.getName().replace(MD1, "").replace(MD2, "");
                yamlTitle = yamlTitle.replace(FILE_NAME, filename);
                filename = URLEncoder.encode(filename, UTF_8);
                yamlTitle = yamlTitle.replace(ABBR_LINK, nameCrcMap.get(filename));

                //do replace
                List<String> mdContents = IOUtils.readLines(in, UTF_8);
                List<String> newdContents = new ArrayList<>(mdContents.size() + 1);
                log.info(yamlTitle);

                newdContents.add(yamlTitle);
                int lines = 0;
                for (String line : mdContents) {
                  String key = "";
                  // 替换配置项 replaces
                  for (Entry<String, String> entry : blog.getReplaces().entrySet()) {
                    key = entry.getKey();
                    if (key.startsWith(SRC)) {
                      String src = entry.getValue();
                      if (line.length() >= src.length() && line.contains(src)) {
                        String target = blog.getReplaces().get(TARGET + key.substring(3));
                        line = line.replace(src, target);
                      }
                    }
                  }
                  // 替换文件名为abbrlink
                  for (Entry<String, String> entry : nameCrcMap.entrySet()) {
                    key = entry.getKey();
                    if (line.length() >= key.length() && line.contains(key)) {
                      line = line.replace(entry.getKey(), entry.getValue());
                    }
                  }
                  newdContents.add(line);
                  lines++;
                  if (lines == LINES_IN_HOME_PAGE) {
                    newdContents.add(READ_MORE);
                  }
                }
                IOUtils.writeLines(newdContents, LN, writer);
              } catch (IOException e) {
                e.printStackTrace();
              }
            });
      }
    }
  }
}
