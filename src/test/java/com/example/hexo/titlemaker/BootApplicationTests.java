package com.example.hexo.titlemaker;

import com.example.hexo.titlemaker.domain.Blog;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * test
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Log
public class BootApplicationTests {

  @Autowired
  Blog blog;

  @Autowired
  ApplicationContext ctx;

  @Test
  public void contextLoads() {
    log.info(blog.toString());

    assert ctx.containsBean("hexoTitleMaker");
    assert blog.getCategories().size() > 0;
    assert blog.getTags().size() > 0;
    assert blog.getTitle() != null;

  }

}
