package br.inf.ufes.ppd;

import java.util.ArrayList;

public class Attack {
	
	private int attackNumber;
	ArrayList<SubAttack> subatacks = new ArrayList<SubAttack>();
	ArrayList<Guess> guesses = new ArrayList<Guess>();
	
	public Attack(int attackNumber) {
		this.attackNumber = attackNumber;
	}
	
	public int getAttackNumber() {
		return attackNumber;
	}
	public void setAttackNumber(int attackNumber) {
		this.attackNumber = attackNumber;
	}
}
