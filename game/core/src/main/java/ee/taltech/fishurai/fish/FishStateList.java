package main.java.ee.taltech.fishurai.fish;

import lombok.NoArgsConstructor;

import java.util.List;

/** Received from the server only; Kryo needs the no-args constructor. */
@NoArgsConstructor
public class FishStateList {
    public List<FishState> states;
}
