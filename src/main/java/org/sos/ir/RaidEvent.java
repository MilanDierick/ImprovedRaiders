/*
 * Copyright (c) 2022 Milan Dierick | This source file is licensed under a modified version of Apache 2.0
 */

package org.sos.ir;

import game.GAME;
import game.faction.FACTIONS;
import game.time.TIME;
import init.race.RACES;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import org.porcupine.modules.IScriptEntity;
import org.porcupine.modules.ISerializable;
import org.porcupine.modules.ITickCapable;
import org.porcupine.statistics.ResourceMetadata;
import org.porcupine.statistics.Statistics;
import org.porcupine.statistics.StockpileStatistics;
import org.porcupine.utilities.UsedImplicitly;
import settlement.main.SETT;
import settlement.stats.STATS;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import util.data.BOOLEAN_OBJECT;
import view.main.MessageText;
import view.main.VIEW;
import view.sett.IDebugPanelSett;
import world.World;
import world.army.WARMYD;
import world.entity.WPathing;
import world.entity.army.WArmy;
import world.map.regions.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.sos.ir.Constants.*;

// TODO: Calculate the chance each tick that a reading army will spawn. Might just use the game's chance calculation.

@UsedImplicitly
public class RaidEvent implements IScriptEntity, ITickCapable, ISerializable {
	private int settlementPopulation;
	private int settlementWealth;
	
	private int valueOfPawn;
	private int valueOfMeleeWeapon;
	private int valueOfRangedWeapon;
	private int valueOfArmor;
	
	private boolean initialized;
	private double nextRaidTimer;
	
	private final StockpileStatistics stockpileStatistics;
	private ArmyBudgetDivision armyBudgetDivision;
	
	private final BOOLEAN_OBJECT<Region> playerFinder = t -> t.faction() == FACTIONS.player();
	
	@UsedImplicitly
	public RaidEvent() {
		IDebugPanelSett.add("Raid Event", this::triggerRaid);
		GAME.events().raider.supress();
		
		initialized = false;
		nextRaidTimer = 0;
		stockpileStatistics = Statistics.get(StockpileStatistics.class);
	}
	
	@Override
	public void onTick(double delta) {
		// We want to make sure that the settlement is present in the game's state.
		// This is mainly to prevent the script from looking at the settlement when it hasn't been created yet.
		// This should be checked in a more concise manner instead of checking which view is active.
		if (VIEW.current() != VIEW.s() && VIEW.current() != VIEW.b() && VIEW.current() != VIEW.world()) {
			return;
		}
		
		// If this is the first time we're updating this script.
		if (!initialized && nextRaidTimer == 0) {
			double raidFrequencyVariationInSeconds = getRandomNumber(RAID_FREQUENCY_DAYS_LOWER,
			                                                         RAID_FREQUENCY_DAYS_HIGHER
			) * TIME.secondsPerDay;
			
			nextRaidTimer = RAID_FREQUENCY_YEARS * DAYS_PER_YEAR * TIME.secondsPerDay + raidFrequencyVariationInSeconds;
			initialized = true;
			
			new MessageText("Improved Raiders", "Improved Raiders version 0.1.0 has been initialized!").send();
		}
		
		nextRaidTimer -= delta;
		
		if (nextRaidTimer <= 0 && Statistics.getPopulationStats().getCurrentPopulationCount() >= 50) {
			triggerRaid();
			
			double raidFrequencyVariationInSeconds = getRandomNumber(RAID_FREQUENCY_DAYS_LOWER,
			                                                         RAID_FREQUENCY_DAYS_HIGHER
			) * TIME.secondsPerDay;
			
			nextRaidTimer = RAID_FREQUENCY_YEARS * DAYS_PER_YEAR * TIME.secondsPerDay + raidFrequencyVariationInSeconds;
		}
	}
	
	/**
	 * Invoked when the object is being serialized.
	 *
	 * @param writer The writer to write to.
	 */
	@Override
	public void onSerialize(FilePutter writer) {
		writer.bool(initialized);
		writer.d(nextRaidTimer);
	}
	
	/**
	 * Invoked when the object is being deserialized.
	 *
	 * @param reader The reader to read from.
	 */
	@Override
	public void onDeserialize(FileGetter reader) {
		try {
			initialized = reader.bool();
			nextRaidTimer = reader.d();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("SameParameterValue")
	private int getRandomNumber(int min, int max) {
		return new Random().nextInt((max - min) + 1) + min;
	}
	
	private void clearCache() {
		settlementPopulation = 0;
		settlementWealth = 0;
	}
	
	private void calculatePopulation() {
		settlementPopulation = STATS.POP().POP.data().get(null);
	}
	
	/**
	 * Calculates the wealth of the settlement, based on the amount of resources in the stockpile.
	 *
	 * @implNote We are using the sell price of the resource, as it is the price that the raiders would receive if they
	 * sold the resource to the markets. Raiders are an impatient folk, and would not want to wait for the resource to
	 * be bought by other settlements, they want their profits NOW!
	 */
	private void calculateSettlementProsperity() {
		for (RESOURCE resource : RESOURCES.ALL()) {
			ResourceMetadata metadata = stockpileStatistics.get(resource);
			settlementWealth += metadata.getStockpile() * metadata.getSellPrice();
		}
		
		settlementWealth += settlementPopulation * valueOfPawn;
		settlementWealth += STATS.GOVERN().RICHES.data().get(null) * SETTLEMENT_RICHES_WEIGHT;
	}
	
	private void calculateBudget() {
		armyBudgetDivision = new ArmyBudgetDivision(settlementWealth);
	}
	
	/**
	 * Calculates the value of a pawn, based on the amount of army supplies it consumes over a certain amount of time,
	 * added on top of the base value of a pawn.
	 */
	private void calculatePawnValue() {
		int valueOfRations = stockpileStatistics.get(RESOURCES.map().tryGet("RATION")).getBuyPrice();
		int valueOfDrinks = stockpileStatistics.get(RESOURCES.map().tryGet("_ALCOHOL")).getBuyPrice();
		int valueOfClothes = stockpileStatistics.get(RESOURCES.map().tryGet("CLOTHES")).getBuyPrice();
		
		float valueOfRationsPerDay = valueOfRations * RATION_CONSUMPTION_PER_DAY;
		float valueOfDrinksPerDay = valueOfDrinks * DRINK_CONSUMPTION_PER_DAY;
		float valueOfClothesPerDay = valueOfClothes * CLOTHES_CONSUMPTION_PER_DAY;
		
		float valueOfRationsTotal = valueOfRationsPerDay * DAYS_PER_YEAR * PAWN_CONSUMPTION_PERIOD;
		float valueOfDrinksTotal = valueOfDrinksPerDay * DAYS_PER_YEAR * PAWN_CONSUMPTION_PERIOD;
		float valueOfClothesTotal = valueOfClothesPerDay * DAYS_PER_YEAR * PAWN_CONSUMPTION_PERIOD;
		
		valueOfPawn = (int) (BASE_PAWN_VALUE + valueOfRationsTotal + valueOfDrinksTotal + valueOfClothesTotal);
	}
	
	/**
	 * Calculates the value of the different types of equipment based on the buy price of the resource.
	 */
	@SuppressWarnings("DuplicateStringLiteralInspection")
	private void calculateEquipmentValue() {
		valueOfMeleeWeapon = stockpileStatistics.get(RESOURCES.map().tryGet("WEAPON")).getBuyPrice();
		valueOfRangedWeapon = stockpileStatistics.get(RESOURCES.map().tryGet("BOW")).getBuyPrice();
		valueOfArmor = stockpileStatistics.get(RESOURCES.map().tryGet("ARMOUR")).getBuyPrice();
	}
	
	/**
	 * Checks if this region is adjacent to a region owned by the player.
	 *
	 * @param region The region where the raid will occur.
	 *
	 * @return a boolean indicating whether this region is suitable to spawn a raid in.
	 */
	private boolean isRegionSuitable(Region region) {
		// If we somehow managed to grab an ill-formed region, return false.
		if (region == null || region.area() == 0) {
			return false;
		}
		
		// If the rebel faction can't create a new army, return false.
		if (!World.ARMIES().rebels().canCreate()) {
			return false;
		}
		
		// If this region already contains a rebel army, return false.
		for (WArmy army : World.ARMIES().rebels().all()) {
			if (army.region() == region) {
				return false;
			}
		}
		
		// If this region is owned by the player, return true.
		if (region.faction() == FACTIONS.player()) {
			return false;
		}
		
		return WPathing.findAdjacentRegion(region, playerFinder) != null;
	}
	
	private boolean spawnArmyInRegion(Region stagingRegion) {
		Region targetRegion = WPathing.findAdjacentRegion(stagingRegion.cx(), stagingRegion.cy(), playerFinder);
		
		if (targetRegion == null) {
			return false;
		}
		
		// Find a random point in the staging region to spawn the army.
		COORDINATE coordinate = WPathing.random(stagingRegion);
		
		// Why are we deducting 1 from the staging region's area?
		WArmy army = World.ARMIES().createRebel(coordinate.x() - 1, coordinate.y() - 1);
		
		if (army == null) {
			return false;
		}
		
		RaiderArmyConstructor constructor = new RaiderArmyConstructor(targetRegion, army);
		
		constructor.setTotalBudget(armyBudgetDivision.getTotalBudget())
		           .setPawnsBudget(armyBudgetDivision.getPawnsBudget())
		           .setWeaponsBudget(armyBudgetDivision.getWeaponsBudget())
		           .setArmourBudget(armyBudgetDivision.getArmourBudget())
		           .setCostPerPawn(valueOfPawn)
		           .setCostPerMeleeWeapon(valueOfMeleeWeapon)
		           .setCostPerRangedWeapon(valueOfRangedWeapon)
		           .setCostPerArmour(valueOfArmor)
		           .configure();
		
		army.name.clear().add(RACES.all().get(0).info.armyNames.rnd());
		
		// Fill the army with supplies, rebel armies should have enough supplies to reach the player's settlement.
		for (WARMYD.WArmySupply supply : WARMYD.supplies().all) {
			supply.current().set(army, supply.max(army));
		}
		
		return true;
	}
	
	private void triggerRaid() {
		clearCache();
		calculatePopulation();
		calculatePawnValue();
		calculateSettlementProsperity();
		calculateEquipmentValue();
		calculateBudget();
		
		List<Region> availableRegions = new ArrayList<>();
		
		for (Region region : World.REGIONS().all()) {
			if (isRegionSuitable(region)) {
				availableRegions.add(region);
			}
		}
		
		if (availableRegions.isEmpty()) {
			throw new IllegalStateException("ImprovedRaiders: no suitable region found to spawn raid in.");
		}
		
		// Randomly mix these regions
		Collections.shuffle(availableRegions);
		
		for (Region region : availableRegions) {
			if (spawnArmyInRegion(region)) {
				break;
			}
		}
		
		int soldierCount = armyBudgetDivision.getPawnsBudget() / valueOfPawn;
		String raiderName = RACES.all().get(0).info.armyNames.rnd();
		
		if (soldierCount < MIN_REBEL_ARMY_SIZE) {
			soldierCount = MIN_REBEL_ARMY_SIZE;
		}
		
		CouncilorMessage councilorMessage = new CouncilorMessage("Councilor", soldierCount);
		
		new RaidersMessage("Raiders!",
		                   SETT.FACTION().capitolRegion().name().toString(),
		                   raiderName,
		                   councilorMessage
		).send();
	}
}
