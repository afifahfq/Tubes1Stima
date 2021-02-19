package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;
import za.co.entelect.challenge.enums.PowerUpType;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {
        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        Cell powerUp = getNearestPowerUp();
        if (powerUp != null) {
            Position powerUpPosition = new Position();
            powerUpPosition.x = powerUp.x;
            powerUpPosition.y = powerUp.y;
            return moveAndDigTo(powerUpPosition);
        }

        Position target;

        target = canBananaBomb();
        if (target.x != -1 && target.y != -1) {
            return new BananaCommand(target);
        }

        target = canSnowBall();
        if (target.x != -1 && target.y != -1) {
            return new SnowballCommand(target);
        }

        Worm huntedWorm = getApproachableOpponent();
        if (huntedWorm != null) {
            return huntStrategy(huntedWorm.id);
        }

        int followerId = currentWorm.id;
        if (followerId != 1) {
            followStrategy(followerId);
        }

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int cellIdx = random.nextInt(surroundingBlocks.size());

        Cell block = surroundingBlocks.get(cellIdx);
        if (block.type == CellType.AIR) {
            return new MoveCommand(block.x, block.y);
        } else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }

        return new DoNothingCommand();
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR ) {
                    break;
                }

                if (cell.occupier != null && cell.occupier.playerId == gameState.myPlayer.id) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) Math.floor(Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    private Cell findCellShortestPath(Position start, Position finish) {
        List<Cell> surrCells = getSurroundingCells(start.x, start.y);
        int[] distance = {0,0,0,0,0,0,0,0};
        for (int i=0; i<surrCells.size(); i++) {
            distance[i] = euclideanDistance(finish.x, finish.y, surrCells.get(i).x, surrCells.get(i).y);
        }
        int distMin = 0;
        int idMin = 0;
        for (int i=0; i<surrCells.size(); i++) {
            distMin = distance[i];
            idMin = i;
            for (int j=0; j<surrCells.size(); j++) {
                if (distance[j] < distMin) {
                    distMin = distance[j];
                    idMin = j;
                }
            }
        }
        return surrCells.get(idMin);
    }

    private boolean checkCell(Cell cell) {
        boolean occupied = false;
        for (MyWorm wormy : gameState.myPlayer.worms) {
            if ((currentWorm.id != wormy.id) && (cell.x == wormy.position.x) && (cell.y == wormy.position.y)) {
                occupied = true;
            }
        }
        return occupied;
    }

    private Command moveAndDigTo(Position finish) {
        Cell nextCell = findCellShortestPath(currentWorm.position, finish);

        while (checkCell(nextCell)) {
            List<Cell> surroundingCells = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
            int cellIdx = random.nextInt(surroundingCells.size());
            nextCell = surroundingCells.get(cellIdx);
        }
        if (nextCell.type == CellType.AIR) {
            return new MoveCommand(nextCell.x, nextCell.y);
        }
        else if (nextCell.type == CellType.DIRT) {
            return new DigCommand(nextCell.x, nextCell.y);
        }
        return new DoNothingCommand();
    }

    private Cell getNearestPowerUp() {
        for (int x=0; x<gameState.mapSize; x++) {
            for (int y=0; y<gameState.mapSize; y++) {
                Cell cell = gameState.map[x][y];
                if (cell.powerup != null) {
                    return cell;
                }
            }
        }
        /* for (Cell cell : gameState.map[coordinateY][coordinateX]) {} */
        return null;
    }

    private Worm getApproachableOpponent() {
        List<Worm> thisOpponent = new ArrayList<>();
        for (Worm enemyWorm : opponent.worms) {
            if (enemyWorm.health > 0) {
                thisOpponent.add(enemyWorm);
            }
        }

//        List<Worm> opponentss = Arrays.asList(gameState.opponents.worms);
//        List<Worm> thisOpponent = opponentss.stream()
//                .filter(w-> w.health > 0).collect(Collectors.toList());

        int[] distance = {0,0,0};
        for (int i = 0; i < thisOpponent.stream().count(); i++) {
            distance[i] = euclideanDistance(currentWorm.position.x, currentWorm.position.y, thisOpponent.get(i).position.x, thisOpponent.get(i).position.y);
        }

        int distMin = 0;
        int idMin = 0;
        for (int i=0; i<thisOpponent.stream().count(); i++) {
            distMin = distance[i];
            idMin = i;
            for (int j=0; j<thisOpponent.stream().count(); j++) {
                if (distance[j] < distMin) {
                    distMin = distance[j];
                    idMin = j;
                }
            }
        }

        return thisOpponent.get(idMin);
    }

    private Command followStrategy(int targetWormId) {
        Worm leaderWorms = gameState.myPlayer.worms[0];
        Worm nearTarget = opponent.worms[0];
        if (leaderWorms.id == targetWormId) {
            if (leaderWorms.health > 0) {
                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, leaderWorms.position.x, leaderWorms.position.y) > 3) {
                    return moveAndDigTo(leaderWorms.position);
                } else {
                    nearTarget = getApproachableOpponent();
                    return moveAndDigTo(nearTarget.position);
                }
            }
            else {
                leaderWorms = gameState.myPlayer.worms[1];
                if (leaderWorms.health > 0) {
                    if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, leaderWorms.position.x, leaderWorms.position.y) > 3) {
                        return moveAndDigTo(leaderWorms.position);
                    } else {
                        nearTarget = getApproachableOpponent();
                        return moveAndDigTo(nearTarget.position);
                    }
                }
                else {
                    nearTarget = getApproachableOpponent();
                    return moveAndDigTo(nearTarget.position);
                }
            }
        }
        return new DoNothingCommand();
    }

    private Command huntStrategy(int targetWormId) {
        for (Worm preyWorm : opponent.worms) {
            if(preyWorm.id == targetWormId){
                if(preyWorm.health > 0){
                    moveAndDigTo(preyWorm.position);
                }
            }
        }
        return new DoNothingCommand();
    }

    private Position canBananaBomb(){
        Position enemy= new Position();
        enemy.x = -1;
        enemy.y = -1;
        int distance;

        if (currentWorm.id==2 ) {
            if (currentWorm.bananaBombs.count>0){
                for (Worm enemyWorm : opponent.worms) {
                    if (enemyWorm.health>0) {
                        distance = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);
                        if (distance <= currentWorm.bananaBombs.range && distance > currentWorm.bananaBombs.damageRadius) {
                            enemy.x = enemyWorm.position.x;
                            enemy.y = enemyWorm.position.y;
                        }
                    }
                }
            }
        }
        return enemy;
    }

    private Position canSnowBall(){
        Position enemy= new Position();
        enemy.x = -1;
        enemy.y = -1;
        int distance;

        if (currentWorm.id==3 ) {
            if (currentWorm.snowballs.count > 0){
                for (Worm enemyWorm : opponent.worms) {
                    if (enemyWorm.health>0) {
                        distance = euclideanDistance(currentWorm.position.x, currentWorm.position.y, enemyWorm.position.x, enemyWorm.position.y);
                        if (distance <= currentWorm.snowballs.range && distance > currentWorm.snowballs.freezeRadius) {
                            enemy.x = enemyWorm.position.x;
                            enemy.y = enemyWorm.position.y;
                        }
                    }
                }
            }
        }
        return enemy;
    }

}
