package main.java.ee.taltech.fishurai.bonus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class BootBonus {
    private final float x;
    private final float y;
    @Setter private boolean isCollected = false;
}
