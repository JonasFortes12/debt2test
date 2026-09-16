package io.github.jonasfortes12.persistence.repository;

/** Per-debt-type totals for a run, for metrics without loading rows. */
public interface DebtTypeCount {
    String getDebtType();

    long getTotal();
}
