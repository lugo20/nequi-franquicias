package co.com.franquicias.model.franchise;

import java.util.List;

public record Franchise(String id, String name, List<Branch> branches) {

    public Franchise {
        branches = branches == null ? List.of() : List.copyOf(branches);
    }
}
