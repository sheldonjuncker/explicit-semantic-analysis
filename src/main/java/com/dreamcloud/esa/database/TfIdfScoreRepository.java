package com.dreamcloud.esa.database;

import com.dreamcloud.esa.tfidf.DocumentScoreReader;
import com.dreamcloud.esa.tfidf.TfIdfScore;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class TfIdfScoreRepository implements DocumentScoreReader {
    Connection con;

    public TfIdfScoreRepository() {

    }

    public int getDocumentFrequency(String term) throws IOException {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement freqStatement = con.prepareStatement("select frequency from esa.df where term = ?");
            freqStatement.setString(1, term);
            ResultSet resultSet = freqStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void saveTermDocumentFrequencies(Map<String, Integer> termDocumentFrequencies) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement freqStatement = con.prepareStatement("insert into esa.df(term, frequency) values(?, ?)");
            int i = 0;
            for (String term : termDocumentFrequencies.keySet()) {
                int count = termDocumentFrequencies.get(term);
                freqStatement.setString(1, term);
                freqStatement.setInt(2, count);
                freqStatement.executeUpdate();
                if (i++ % 1000 == 0) {
                    System.out.println("Saved document frequency: [" + term + "\t" + count + "]");
                }
            }
        } catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void getTfIdfScores(String[] terms, Vector<TfIdfScore> outVector) throws IOException {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            StringBuilder sql = new StringBuilder("select term, document, score from esa.score where term IN (");
            for (int i = 0; i < terms.length; i++) {
                sql.append('?');
                if (i < terms.length - 1) {
                    sql.append(", ");
                }
            }
            sql.append(')');
            PreparedStatement scoreStatement = con.prepareStatement(sql.toString());
            int i = 1;
            for (String term : terms) {
                scoreStatement.setString(i++, term);
            }
            ResultSet resultSet = scoreStatement.executeQuery();
            while (resultSet.next()) {
                String term = resultSet.getString(1);
                int document = resultSet.getInt(2);
                double score = resultSet.getDouble(3);
                outVector.add(new TfIdfScore(document, term, score));
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void getTfIdfScores(String term, Vector<TfIdfScore> outVector) throws IOException {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement scoreStatement = con.prepareStatement("select document, score from esa.score where term = ? order by score desc");
            scoreStatement.setString(1, term);
            ResultSet resultSet = scoreStatement.executeQuery();
            while (resultSet.next()) {
                int document = resultSet.getInt(1);
                double score = resultSet.getDouble(2);
                outVector.add(new TfIdfScore(document, term, score));
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void saveTfIdfScores(int document, TfIdfScore[] scores) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement scoreStatement = con.prepareStatement("insert into esa.score(document, term, score) values(?, ?, ?)");
            for (TfIdfScore score : scores) {
                scoreStatement.setInt(1, document);
                scoreStatement.setString(2, score.getTerm());
                if (Double.isNaN(score.getScore())) {
                    System.out.println("Found a NaN: " + score.getTerm() + ", " + document);
                }
                scoreStatement.setDouble(3, score.getScore());
                scoreStatement.executeUpdate();
            }
        } catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String[] getTerms() {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }
            long termCount = 0;
            PreparedStatement countStatement = con.prepareStatement("select count(*) from esa.df");
            ResultSet resultSet = countStatement.executeQuery();
            ArrayList<TfIdfScore> scores = new ArrayList<>();
            if (resultSet.next()) {
                termCount = resultSet.getLong(1);
            }

            if (termCount > 0) {
                String[] terms = new String[(int) termCount];
                PreparedStatement termSelect = con.prepareStatement("select term from esa.df;");
                resultSet = termSelect.executeQuery();
                int i = 0;
                while (resultSet.next()) {
                    terms[i++] = resultSet.getString(1);
                }
                return terms;
            } else {
                return null;
            }
        } catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public void pruneTerm(String term, double cutoffScore) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }
            PreparedStatement pruneUpdate = con.prepareStatement("update esa.score set score = 0 where term = ? and score < ?");
            pruneUpdate.setString(1, term);
            pruneUpdate.setDouble(2, cutoffScore);
            pruneUpdate.executeUpdate();
        } catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public int getDocumentCount() throws IOException {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            Statement freqStatement = con.createStatement();
            ResultSet resultSet = freqStatement.executeQuery("select num_docs from esa.num_docs");
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    public Map<String, Integer> getDocumentFrequencies() throws IOException {
        try {
            Map<String, Integer> documentFrequencies = new HashMap<>();
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            Statement freqStatement = con.createStatement();
            ResultSet resultSet = freqStatement.executeQuery("select * from esa.df");
            while (resultSet.next()) {
                String term = resultSet.getString(1);
                int frequency = resultSet.getInt(2);
                documentFrequencies.put(term, frequency);
            }
            return documentFrequencies;
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void saveDocumentCount(int numDocs) {
        try {
            if (this.con == null) {
                this.con = MySQLConnection.getConnection();
            }

            PreparedStatement numDocsInsert = con.prepareStatement("insert into esa.num_docs(num_docs) values(?)");
            numDocsInsert.setInt(1, numDocs);
            numDocsInsert.executeUpdate();
        } catch (Exception e) {
            System.out.println("Postgres is unhappy about something:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public double getAverageDocumentLength() {
        //todo: implement
        return 1;
    }
}

