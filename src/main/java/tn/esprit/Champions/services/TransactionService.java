package tn.esprit.Champions.services;

import tn.esprit.Champions.models.transaction;
import tn.esprit.Champions.models.wallet;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.List;

public class TransactionService implements CRUD<transaction>{

    private Connection cnx;
    public TransactionService()
    {
        cnx = DbConnection.getInstance().getCnx();
    }
    @Override
    public void insertOne(transaction transaction) throws SQLException {

    }


    @Override
    public void updateOne(transaction transaction) throws SQLException {

    }

    @Override
    public void deleteOne(transaction transaction) throws SQLException {

    }

    @Override
    public List<transaction> SelectAll() throws SQLException {
        return List.of();
    }
}