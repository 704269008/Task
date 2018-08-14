package com.fgh.task2.service;


import com.fgh.task2.Utils.cache.RedisUtils;
import com.fgh.task2.dao.UserDao;
import com.fgh.task2.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service("userServiceImplRedis")
public class UserServiceImplRedis implements UserService{
    Logger logger=LoggerFactory.getLogger(UserServiceImplRedis.class);
    private static final String Key="userList"; //List key

    @Resource
    private UserDao userDao;
    @Resource
    private RedisUtils redisUtils;


    public List<User> findUserBy(User user)throws Exception{
        return userDao.findUserBy(user);
    }

    public User findUserById (int id)throws Exception{
        logger.debug("------------------通过用户ID查找---------------");
        String s_id=String.valueOf(id);
        Object object=redisUtils.getkey(s_id);
        logger.debug("缓存值："+object);
        logger.debug("能接到get吗   Id:"+s_id );
        if (object!=null) {
            logger.debug("findId缓存读取 user" + id);
            return (User) object;
        }else {
            logger.debug("数据库查询 id:"+s_id);
            User user=userDao.findUserById(id);
            if (user!=null){
                logger.debug("添加数据库记录 id："+s_id);
                redisUtils.add(s_id,userDao.findUserById(id));
                return  user;
            }else {
            logger.debug("处理缓存击穿 id:"+s_id);
                redisUtils.add(s_id,"breakdown");
                Object object1=redisUtils.getkey(s_id);
                logger.debug("击穿缓存值："+object1);
                return null;
            }
        }
    }


    public Integer insertUser(User user)throws Exception{
        return userDao.insertUser(user);
    }

    @Override
    public Boolean delUser(int id)throws Exception{
        logger.debug("-----------删除-------------");
        Boolean delFlag=userDao.delUser(id);
        if (delFlag){
            String s_id=String.valueOf(id);
            logger.debug(" 删除相应id的缓存 s_id:"+s_id);
            redisUtils.delKey(s_id);
            redisUtils.delKey(Key);
        }

        return delFlag;
    }
    public Boolean updateUser(User user)throws Exception{
        logger.debug("--------------更新数据，删除缓存--------------");
        Boolean updateFlag=userDao.updateUser(user);
        if(updateFlag){
            String s_id=String.valueOf(user.getId());
            logger.debug("update 删除相应id的缓存 s_id:"+s_id);
            redisUtils.delKey(s_id);
            redisUtils.delKey(Key);
        }
        return  updateFlag;
    }
    public List<User> findAll()throws Exception{
        logger.debug("----------判读Key是否存在---------");
        if ( redisUtils.expKey(Key)) {
            logger.debug("缓存中读取结果 List");
            Object users=redisUtils.getJsonString(Key,User.class);
            return (List<User>)users;
        }else {
            logger.debug("缓存中无相应List，执行数据库查询");
            List <User> users = userDao.findAll();
            logger.debug("查询结果放入缓存，Key: "+Key);
            redisUtils.setJsonString(users, Key);
            return userDao.findAll();
        }

    }
}
