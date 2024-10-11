package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

public class BatteryUpgradeEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
    public long amount;

    public BatteryUpgradeEnergyStorage(long amount) {
        this.amount = amount;
    }

    @Override
    protected Long createSnapshot() {
        return amount;
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        amount = snapshot;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);

        if (maxAmount > 0) {
            updateSnapshots(transaction);
            amount += maxAmount;
            return maxAmount;
        }

        return 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);

        if (maxAmount > 0) {
            updateSnapshots(transaction);
            amount -= maxAmount;
            return maxAmount;
        }

        return 0;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public long getCapacity() {
        return -1;
    }
}
