package com.baomidou.mybatisplus.test.h2;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.baomidou.mybatisplus.mapper.Condition;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.test.h2.config.DBConfig;
import com.baomidou.mybatisplus.test.h2.config.MybatisConfigMetaObjOptLockConfig;
import com.baomidou.mybatisplus.test.h2.entity.mapper.H2UserVersionAndLogicDeleteMapper;
import com.baomidou.mybatisplus.test.h2.entity.persistent.H2UserVersionAndLogicDeleteEntity;

/**
 * <p>
 * </p>
 *
 * @author yuxiaobin
 * @date 2017/6/29
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DBConfig.class, MybatisConfigMetaObjOptLockConfig.class})
public class H2MetaObjAndVersionAndOptLockTest extends H2Test{

    @BeforeClass
    public static void initDB() throws SQLException, IOException {
        @SuppressWarnings("resource")
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(DBConfig.class);
        context.register(MybatisConfigMetaObjOptLockConfig.class);
        context.refresh();
        DataSource ds = (DataSource) context.getBean("dataSource");
        try (Connection conn = ds.getConnection()) {
            String createTableSql = readFile("user.ddl.sql");
            Statement stmt = conn.createStatement();
            stmt.execute(createTableSql);
            stmt.execute("truncate table h2user");
            executeSql(stmt, "user.insert.sql");
            conn.commit();
        }
    }

    @Autowired
    H2UserVersionAndLogicDeleteMapper userMapper;

    @Test
    public void testInsert(){
        Long id = 991L;
        H2UserVersionAndLogicDeleteEntity user = new H2UserVersionAndLogicDeleteEntity();
        user.setId(id);
        user.setName("991");
        user.setAge(91);
        user.setPrice(BigDecimal.TEN);
        user.setDesc("asdf");
        user.setTestType(1);
        user.setVersion(1);
        userMapper.insertAllColumn(user);

        H2UserVersionAndLogicDeleteEntity userDB = userMapper.selectById(id);
        Assert.assertEquals(null, userDB.getTestDate());

        userDB.setName("991");
        userMapper.updateById(userDB);

        userDB = userMapper.selectById(id);
        Assert.assertEquals("991", userDB.getName());
    }

    @Test
    public void testUpdateByEntityWrapperNoDateVersion() {
        Long id = 992L;
        H2UserVersionAndLogicDeleteEntity user = new H2UserVersionAndLogicDeleteEntity();
        user.setId(id);
        user.setName("992");
        user.setAge(92);
        user.setPrice(BigDecimal.TEN);
        user.setDesc("asdf");
        user.setTestType(1);
        user.setVersion(1);
        userMapper.insertAllColumn(user);

        H2UserVersionAndLogicDeleteEntity userDB = userMapper.selectById(id);

        H2UserVersionAndLogicDeleteEntity updUser = new H2UserVersionAndLogicDeleteEntity();
        updUser.setName("999");

        userMapper.update(updUser, new EntityWrapper<>(userDB));

        userDB = userMapper.selectById(id);
        Assert.assertEquals("999", userDB.getName());
    }

    @Test
    public void testUpdateByIdWithDateVersion() {
        Long id = 994L;
        H2UserVersionAndLogicDeleteEntity user = new H2UserVersionAndLogicDeleteEntity();
        user.setId(id);
        user.setName("994");
        user.setAge(91);
        user.setPrice(BigDecimal.TEN);
        user.setDesc("asdf");
        user.setTestType(1);
        user.setVersion(1);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        user.setTestDate(cal.getTime());
        userMapper.insertAllColumn(user);

        System.out.println("before update: testDate=" + user.getTestDate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        H2UserVersionAndLogicDeleteEntity userDB = userMapper.selectById(id);

        Assert.assertNotNull(userDB.getTestDate());
        String originalDateVersionStr = sdf.format(cal.getTime());
        Assert.assertEquals(originalDateVersionStr, sdf.format(userDB.getTestDate()));

        userDB.setName("991");
        userMapper.updateById(userDB);
        userDB = userMapper.selectById(id);
        Assert.assertEquals("991", userDB.getName());
        Date versionDate = userDB.getTestDate();
        System.out.println("after update: testDate=" + versionDate);
        String versionDateStr = sdf.format(versionDate);
        Assert.assertEquals(sdf.format(new Date()), versionDateStr);

        Assert.assertNotEquals(originalDateVersionStr, versionDateStr);

    }

    @Test
    public void testUpdateByEntityWrapperWithDateVersion() {
        Long id = 993L;
        H2UserVersionAndLogicDeleteEntity user = new H2UserVersionAndLogicDeleteEntity();
        user.setId(id);
        user.setName("992");
        user.setAge(92);
        user.setPrice(BigDecimal.TEN);
        user.setDesc("asdf");
        user.setTestType(1);
        user.setVersion(1);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        user.setTestDate(cal.getTime());
        userMapper.insertAllColumn(user);

        H2UserVersionAndLogicDeleteEntity userDB = userMapper.selectById(id);

        H2UserVersionAndLogicDeleteEntity updUser = new H2UserVersionAndLogicDeleteEntity();
        updUser.setName("999");
        userDB.setVersion(null);
        userMapper.update(updUser, new EntityWrapper<>(userDB));

        System.out.println("before update: testDate=" + userDB.getTestDate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");

        userDB = userMapper.selectById(id);
        Assert.assertEquals("999", userDB.getName());

        Date versionDate = userDB.getTestDate();
        System.out.println("after update: testDate=" + versionDate);
        String versionDateStr = sdf.format(versionDate);
        Assert.assertEquals(sdf.format(new Date()), versionDateStr);
    }


    @Test
    public void testLogicDeleted() {
        H2UserVersionAndLogicDeleteEntity user = new H2UserVersionAndLogicDeleteEntity();
        user.setAge(1);
        user.setPrice(new BigDecimal("9.99"));
        user.setVersion(-1);
        userMapper.insert(user);
        Long id = user.getId();
        Assert.assertNotNull(id);
        Assert.assertNotNull(userMapper.selectList(Condition.create().orderBy("age")));
        H2UserVersionAndLogicDeleteEntity userFromDB = userMapper.selectById(user.getId());
        Assert.assertNull(userFromDB);
    }
}
