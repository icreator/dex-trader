package org.erachain.dextrader;

import org.apache.log4j.PropertyConfigurator;
import org.erachain.dextrader.controller.Controller;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//@SpringBootApplication
public class Start {

    public static void main(String[] args) {

        String log4JPropertyFile = "resources/log4j" + (Controller.DEVELOP_USE? "-dev": "") + ".properties";
        Properties p = new Properties();

        try {
            p.load(new FileInputStream(log4JPropertyFile));
            PropertyConfigurator.configure(p);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        //SpringApplicationBuilder builder = new SpringApplicationBuilder(Start.class);

        //builder.headless(false).run(args);

        Controller.getInstance().startApplication(args);

        ///SpringApplication.run(Start.class, args);

    }

}
