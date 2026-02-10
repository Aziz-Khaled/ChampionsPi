package tn.esprit.Champions.services;


import tn.esprit.Champions.models.Asset;

import java.sql.SQLException;
import java.util.List;

public interface CRUD <T>{

    public void insertOne(T t) throws SQLException;

    public void updateOne(T t) throws SQLException;

    public void deleteOne(T t) throws SQLException;

    public List<T> SelectAll() throws SQLException;

}