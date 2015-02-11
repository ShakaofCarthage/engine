package com.eaw1805.core.payment;

import com.eaw1805.data.model.Game;

import java.util.Date;

/**
 * Simple POJO for storing a game turn - keeping information such as the date of the processing and the cost.
 */
class GameTurn {

    private Game game;

    private Date date;

    private int cost;

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

}

