package fr.horizonsmp.jeirecipefix.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientBrandTest {

    @Test
    void detectsFabric() {
        assertEquals(ClientBrand.FABRIC, ClientBrand.fromBrand("fabric"));
        assertEquals(ClientBrand.FABRIC, ClientBrand.fromBrand("Fabric"));
    }

    @Test
    void detectsNeoForge() {
        assertEquals(ClientBrand.NEOFORGE, ClientBrand.fromBrand("neoforge"));
    }

    @Test
    void treatsVanillaAndNullAsOther() {
        assertEquals(ClientBrand.OTHER, ClientBrand.fromBrand("vanilla"));
        assertEquals(ClientBrand.OTHER, ClientBrand.fromBrand(null));
    }

    @Test
    void onlyFabricAndNeoForgeAreSupported() {
        assertTrue(ClientBrand.FABRIC.isSupported());
        assertTrue(ClientBrand.NEOFORGE.isSupported());
        assertFalse(ClientBrand.OTHER.isSupported());
    }
}
