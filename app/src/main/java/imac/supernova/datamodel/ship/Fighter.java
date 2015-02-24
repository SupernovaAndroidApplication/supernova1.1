package imac.supernova.datamodel.ship;

import imac.supernova.datamodel.Player;

/**
 * Created by Clara on 05/02/2015.
 */
public class Fighter extends Ship {

    public Fighter(Player owner){
        super(owner);
        maxHealth = 2;
        health = maxHealth;
        maxMove = 5;
        damage = 1;
        range = 1;
    }
}
