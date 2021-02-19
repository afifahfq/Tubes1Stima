package za.co.entelect.challenge.command;

import za.co.entelect.challenge.entities.Position;

public class SnowballCommand implements Command{
    private final int x;
    private final int y;

    public SnowballCommand(Position position) {
        this.x = position.x;
        this.y = position.y;
    }

    @Override
    public String render() {
        return String.format("snowball %d %d", x, y);
    }
}
