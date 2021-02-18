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
    private MyPlayer myPlayer;

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
            if (cells.contains(enemyPosition)) {
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
                if (cell.type != CellType.AIR) {
                    break;
                }

                if (cell.occupier.playerId == myPlayer.id) {
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
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
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
        for (int x=0; x<33; x++) {
            for (int y=0; y<33; y++) {
                Cell cell = gameState.map[x][y];
                if (cell.powerup != null) {
                    return cell;
                }
            }
        }
        /* for (Cell cell : gameState.map[coordinateY][coordinateX]) {} */
        return null;
    }
}
