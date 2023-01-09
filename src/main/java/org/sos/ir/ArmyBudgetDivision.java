/*
 * Copyright (c) 2022 Milan Dierick | This source file is licensed under a modified version of Apache 2.0
 */

package org.sos.ir;

import static org.sos.ir.Constants.*;

public class ArmyBudgetDivision {
	private final int totalBudget;
	private final int pawnsBudget;
	private final int weaponsBudget;
	private final int armourBudget;
	
	public ArmyBudgetDivision(int totalBudget) {
		this.totalBudget = (int) (totalBudget * EXPECTED_GARRISON_INVESTMENT);
		this.pawnsBudget = (int) (this.totalBudget * PAWNS_PERCENTAGE_OF_TOTAL_BUDGET);
		this.weaponsBudget = (int) (this.totalBudget * WEAPONS_PERCENTAGE_OF_TOTAL_BUDGET);
		this.armourBudget = (int) (this.totalBudget * ARMOUR_PERCENTAGE_OF_TOTAL_BUDGET);
	}
	
	public int getTotalBudget() {
		return totalBudget;
	}
	
	public int getPawnsBudget() {
		return pawnsBudget;
	}
	
	public int getWeaponsBudget() {
		return weaponsBudget;
	}
	
	public int getArmourBudget() {
		return armourBudget;
	}
}
