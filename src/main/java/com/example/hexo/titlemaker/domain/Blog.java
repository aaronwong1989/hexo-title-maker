package com.example.hexo.titlemaker.domain;

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 需要生成Hexo title的Blog实体类
 *
 * @author Aaron
 */
@Data
@Component
@ConfigurationProperties("hexo")
public class Blog {

  public final static String TITLE = "title: ";
  public final static String ABBRLINK = "abbrlink: ";
  public final static String TAGS = "tags: ";
  public final static String CATEGORIES = "categories: ";
  public final static String TOP = "top: ";

  private String title;
  private List<String> tags;
  private List<String> categories;
  private String top;
  private String mdPath;
  private Map<String, String> replaces;
  private int moreIndex;

}
