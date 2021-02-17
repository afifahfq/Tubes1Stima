package za.co.entelect.challenge.command;

import za.co.entelect.challenge.entities.Opponent;
import za.co.entelect.challenge.entities.Position;
import za.co.entelect.challenge.entities.Worm;

public class HuntStrategy implements Command{

    private char hunt;
    private Opponent targetWormId;
    private Opponent[] opponent;
    private Position position;
    private Opponent w;
    private Worm health;

    public void huntStrategy(int targetWormId) {
        this.opponent = opponent.stream().filter(w -> w.health > 0).find(w -> w === targetWormId);
        DigCommand(opponent.position.x,opponent.position.y );
        MoveCommand(opponent.position.x,opponent.position.y );
    }
}
