package za.co.entelect.challenge.command;

import za.co.entelect.challenge.entities.MyPlayer;
import za.co.entelect.challenge.entities.Worm;

public class FollowStrategy {
    private Worm targetWormId;

    public void followStrategy(Worm targetWormId) {
        follow ();
        MyPlayer.Worm.stream().filter(w -> w.health > 0).find(w -> w.id == targetWormId);
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }
    
    public void follow () {
        Worm leaderWorm = MyPlayer.stream().find(w -> w.id == targetWormId);
        if (euclideanDistance(MyPlayer.position, leaderWorm.position) > 3) {
            DigCommand(nearTarget.worm.position);
        }
        Worm nearTarget = getApproachableOpponent();
        DigCommand(nearTarget.worm.position);
        MoveCommand(nearTarget.worm.position);
    }
}
