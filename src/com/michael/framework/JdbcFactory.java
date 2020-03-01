/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.michael.framework;

import com.michael.framework.exception.JdbcConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author michael
 */
public class JdbcFactory {

    public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String MARIADB_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    public static final String ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";

    private HashMap<String ,DatabaseInformation> databasesInformations = new HashMap<String, DatabaseInformation>();

    public HashMap<String, DatabaseInformation> getDatabasesInformations() {
        return databasesInformations;
    }

    public JdbcFactory() throws ParserConfigurationException, IOException, SAXException {
        InputStream in = JdbcFactory.class.getResourceAsStream("/genericDao.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(in);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("database");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                DatabaseInformation dbInfo = new DatabaseInformation();
                dbInfo.setDatabaseId(element.getElementsByTagName("id").item(0).getTextContent());
                if(databasesInformations.containsKey(dbInfo.getDatabaseId())) {
                    throw new RuntimeException("Duplicata d'id: Il existe déja une connexion de base de donnée avec l'id " + dbInfo.getDatabaseId());
                }
                
                dbInfo.setUrl(element.getElementsByTagName("url").item(0).getTextContent());
                dbInfo.setUsername(element.getElementsByTagName("username").item(0).getTextContent());
                dbInfo.setPassword(element.getElementsByTagName("password").item(0).getTextContent());
                
                databasesInformations.put(dbInfo.getDatabaseId(), dbInfo);
            }
        }

        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new JdbcConfigurationException("Le driver " + MYSQL_DRIVER + " est introuvable dans le classpath.");
        }

        try {
            Class.forName(MARIADB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new JdbcConfigurationException("Le driver " + MARIADB_DRIVER + " est introuvable dans le classpath.");
        }

        try {
            Class.forName(POSTGRESQL_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new JdbcConfigurationException("Le driver " + POSTGRESQL_DRIVER + " est introuvable dans le classpath.");
        }

        try {
            Class.forName(ORACLE_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new JdbcConfigurationException("Le driver " + ORACLE_DRIVER + " est introuvable dans le classpath.");
        }
    }

    public Connection getConnection(String dbId) throws SQLException {
        return DriverManager.getConnection(databasesInformations.get(dbId).getUrl(), databasesInformations.get(dbId).getUsername(), databasesInformations.get(dbId).getPassword());
    }
}
