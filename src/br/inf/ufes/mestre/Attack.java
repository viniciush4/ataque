package br.inf.ufes.mestre;

import java.util.ArrayList;

import br.inf.ufes.ppd.Guess;

public class Attack {
	
	private int attackNumber;
	private int quantidadeSubataquesEmAndamento;
	ArrayList<Guess> guesses = new ArrayList<Guess>();
	
	public Attack(int attackNumber) {
		this.attackNumber = attackNumber;
	}
	
	public int getAttackNumber() {
		return attackNumber;
	}

	public int getQuantidadeSubataquesEmAndamento() {
		return quantidadeSubataquesEmAndamento;
	}

	public void incrementaSubataquesEmAndamento() {
		this.quantidadeSubataquesEmAndamento++;
	}
	
	public void decrementaSubataquesEmAndamento() {
		this.quantidadeSubataquesEmAndamento--;
	}
}
