package com.palmergames.bukkit.towny;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockEconomy extends AbstractEconomy {

    private Map<String, Double> accounts = new HashMap<>();
    private Map<String, Double> bank = new HashMap<>();

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "x";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double v) {
        return v + "";
    }

    @Override
    public String currencyNamePlural() {
        return "";
    }

    @Override
    public String currencyNameSingular() {
        return "";
    }

    @Override
    public boolean hasAccount(String s) {
        return accounts.containsKey(s);
    }

    @Override
    public boolean hasAccount(String s, String s1) {
        return hasAccount(s+s1);
    }

    @Override
    public double getBalance(String s) {
        return accounts.get(s);
    }

    @Override
    public double getBalance(String s, String s1) {
        return getBalance(s+s1);
    }

    @Override
    public boolean has(String s, double v) {
        return getBalance(s) >= v;
    }

    @Override
    public boolean has(String s, String s1, double v) {
        return has(s+s1, v);
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, double v) {
        accounts.put(s, accounts.get(s) - v);
        return new EconomyResponse(v, getBalance(s), EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, String s1, double v) {
        return withdrawPlayer(s+s1, v);
    }

    @Override
    public EconomyResponse depositPlayer(String s, double v) {
        accounts.put(s, accounts.get(s) + v);
        return new EconomyResponse(v, getBalance(s), EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(String s, String s1, double v) {
        return depositPlayer(s+s1,v);
    }

    @Override
    public EconomyResponse createBank(String s, String s1) {
        return new EconomyResponse(100000, 100000, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse deleteBank(String s) {
        return new EconomyResponse(100000, 100000, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankBalance(String s) {
        return new EconomyResponse(100000, 100000, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankHas(String s, double v) {
        return new EconomyResponse(100000, 100000, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankWithdraw(String s, double v) {
        return new EconomyResponse(100000, 100000, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse bankDeposit(String s, double v) {
        return new EconomyResponse(100000, 100000, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse isBankOwner(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String s, String s1) {
        return new EconomyResponse(100000, 100000, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String s) {
        if (accounts.containsKey(s)) {
            return false;
        }
        accounts.put(s, 0D);
        return true;
    }

    @Override
    public boolean createPlayerAccount(String s, String s1) {
        return createPlayerAccount(s+s1);
    }
}
