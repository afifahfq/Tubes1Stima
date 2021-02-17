package za.co.entelect.challenge.command;

import za.co.entelect.challenge.entities.Position;
import za.co.entelect.challenge.entities.Weapon;
import za.co.entelect.challenge.entities.Worm;

public class HuntStrategy implements  Command{

    private char hunt;
    private Worm targetWormId;
    private Worm opponent;
    private Position position;
    private Worm w;
    private Weapon health;

    public void huntStrategy(int targetWormId) {
            getOpponent();
    }
    
    public void getOpponent () {
        this.opponent = opponent.filter(w -> w.health > 0).find(w -> w === targetWormId);
        return digAndMoveTo(opponent.position);
    }
}
