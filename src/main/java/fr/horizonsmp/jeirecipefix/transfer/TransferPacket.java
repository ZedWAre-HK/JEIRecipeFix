package fr.horizonsmp.jeirecipefix.transfer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

record TransferPacket(
        List<Operation> operations,
        List<Integer> craftingSlots,
        List<Integer> inventorySlots,
        boolean maxTransfer,
        boolean requireCompleteSets
) {
    private static final int MAX_LIST_SIZE = 256;

    record Operation(int inventorySlot, int craftingSlot, int count) {}

    static TransferPacket decode(byte[] data, boolean counted) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        int operationCount = checkedSize(VarInts.read(input));
        List<Operation> operations = new ArrayList<>(operationCount);
        for (int i = 0; i < operationCount; i++) {
            int inventorySlot = VarInts.read(input);
            int craftingSlot = VarInts.read(input);
            int count = counted ? VarInts.read(input) : 1;
            if (count < 1 || count > 64) throw new IOException("Invalid transfer count: " + count);
            operations.add(new Operation(inventorySlot, craftingSlot, count));
        }
        List<Integer> craftingSlots = readIntList(input);
        List<Integer> inventorySlots = readIntList(input);
        boolean maxTransfer = readBoolean(input);
        boolean requireCompleteSets = readBoolean(input);
        if (input.available() != 0) throw new IOException("Trailing bytes in JEI packet");
        return new TransferPacket(operations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets);
    }

    private static List<Integer> readIntList(ByteArrayInputStream input) throws IOException {
        int size = checkedSize(VarInts.read(input));
        List<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) values.add(VarInts.read(input));
        return values;
    }

    private static int checkedSize(int size) throws IOException {
        if (size < 0 || size > MAX_LIST_SIZE) throw new IOException("Invalid list size: " + size);
        return size;
    }

    private static boolean readBoolean(ByteArrayInputStream input) throws IOException {
        int value = input.read();
        if (value != 0 && value != 1) throw new IOException("Invalid boolean: " + value);
        return value == 1;
    }
}
