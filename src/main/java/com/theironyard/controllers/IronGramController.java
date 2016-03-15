package com.theironyard.controllers;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import com.theironyard.services.PhotoRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utils.PasswordStorage;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by MacLap on 3/15/16.
 */

@RestController
public class IronGramController {

    @Autowired
    PhotoRepository photos;

    @Autowired
    UserRepository users;

    Server dbui = null;

    @PostConstruct
    public void init() throws SQLException {
        dbui = Server.createWebServer().start();
    }

    @PreDestroy
    public void destroy(){
        dbui.stop();
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public User login(String username, String password, HttpSession session, HttpServletResponse response) throws Exception {
        User user = users.findByName(username);
        if (user == null){
            user = new User(username, PasswordStorage.createHash(password));
            users.save(user);
        }

        else if (!PasswordStorage.verifyPassword(password, user.getPasswordHash())) {
          throw new Exception("wrong password");
        }
        session.setAttribute("username", username);
        response.sendRedirect("/");
        return user;
    }

    @RequestMapping(path = "/user", method = RequestMethod.GET)
    public User getUser(HttpSession session) {
        String username = (String) session.getAttribute("username");
        return users.findByName(username);
    }

    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public Photo upload(MultipartFile photo, String recipient, HttpSession session, HttpServletResponse response) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null){
            throw new Exception("Not Logged in.");
        }

        User user = users.findByName(username);

        File photoFile = File.createTempFile("image", photo.getOriginalFilename(), new File("public"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Photo p = new Photo(user, (users.findByName(recipient) == null) ? null : users.findByName(recipient), photoFile.getName());
        photos.save(p);

        response.sendRedirect("/");
        return p;
    }

    @RequestMapping(path = "/photos", method = RequestMethod.GET)
    public List<Photo> showPhotos(HttpSession session){
        User user = users.findByName((String) session.getAttribute("username"));
        List<Photo> allPhotos = (List<Photo>) photos.findAll();
        List<Photo> listPhotos = photos.findByRecipient(user);

        for (Photo photo: allPhotos) {
            if (photo.getRecipient() == null) {
                listPhotos.add(photo);
            }
        }

        for (Photo photo : listPhotos) {
            if (photo.getTime() == null) {
                photo.setTime(LocalDateTime.now());
                photos.save(photo);
            }
            if (LocalDateTime.now().isAfter(photo.getTime().plusSeconds(10))){

                File f = new File("public/" + photo.getFilename());
                f.delete();
                photos.delete(photo);
            }
        }
        return listPhotos;
    }

    @RequestMapping(path = "/logout", method = RequestMethod.POST)
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate();
        response.sendRedirect("/");
    }
}
