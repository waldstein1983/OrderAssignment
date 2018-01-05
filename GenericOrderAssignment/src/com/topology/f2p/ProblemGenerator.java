package com.topology.f2p;

import java.util.*;

public class ProblemGenerator {
    final int LARGE_POSITIVE = 10000000;
    final double INTEGER_GAP = 0.0001;
    final int STD_DISTANCE = 2000;
    final int SAME_DISTANCE = 300;
    final int NEXT_DISTANCE = 800;
    final double ALPHA = 0.1;
    final double BETA = 0.3;
    final double UNIT_DISTANCE_COST = 0.05;
    final int FIRST_K_CLOSEST = 7;
    Sortable sortable;
    List<FC> FCs = new ArrayList<>();
    List<Order> orders = new ArrayList<>();
    Map<String, Order> idOrders = new HashMap<>();
    Map<Integer, List<Order>> hourOrders = new HashMap<>();
    List<ASIN> ASINs = new ArrayList<>();
    double max_X = 10000;
    double max_Y = 10000;
    Map<Order, Map<FC, Double>> sortedShippingDistances = new HashMap<>();
    Map<Order, Map<FC, Double>> sortedShippingCosts = new HashMap<>();
    Map<Order, List<Shipment>> shipments = new HashMap<>();
//    XPRB bcl = new XPRB();
//    XPRBprob problem = bcl.newProb("Problem");

    public ProblemGenerator(Sortable sortable) {
        this.sortable = sortable;
    }

    public static void main(String[] args) {
        ProblemGenerator generator = new ProblemGenerator(Sortable.SORTABLE);
        generator.buildASINs(700);
        generator.buildFCs(10);
        generator.buildOrders(100);
        generator.solveByHeuristic();

//        generator.buildModel();
    }

    void buildASINs(int size) {
        for (int i = 0; i < size; i++) {
            String id = "" + i;
            double weight = Math.random() * 20 + 1;
            ASIN asin = new ASIN(id, sortable, weight);
            ASINs.add(asin);
        }
    }

    void buildOrders(int size) {
        for (int i = 0; i < size; i++) {
            double x = Math.random() * max_X;
            double y = Math.random() * max_Y;

            Map<FC, Double> FCDistance = new HashMap<>();
            for (FC fc : FCs) {
                double shippingCost = ShippingCost.EclidDistance(x, y, fc.x, fc.y);
                FCDistance.put(fc, shippingCost);
            }

            Map<FC, Double> sorted = Utils.sortByValue(FCDistance);

            double closestDistance = (double) sorted.values().toArray()[0];


            ShipOption option;
            double random = Math.random();
            if (random < 0.4) {
                option = ShipOption.STD;
            } else if (random >= 0.4 && random <= 0.8) {
                if (closestDistance <= SAME_DISTANCE)
                    option = ShipOption.SAME;
                else if (closestDistance > SAME_DISTANCE && closestDistance <= NEXT_DISTANCE) {
                    option = ShipOption.NEXT;
                } else {
                    option = ShipOption.STD;
                }
            } else {
                if (closestDistance > NEXT_DISTANCE) {
                    option = ShipOption.STD;
                } else
                    option = ShipOption.NEXT;
            }

            int orderSize = (int) (Math.random() * 3 + 1);
//            int orderSize = 1;
            Map<ASIN, Integer> units = new HashMap<>();
            List<ASIN> selectedASINs = new LinkedList<>();
            for (int j = 0; j < orderSize; j++) {
                int targetId = (int) (Math.random() * ASINs.size());
                ASIN target = ASINs.get(targetId);
                while (selectedASINs.contains(target)) {
                    targetId = (int) (Math.random() * ASINs.size());
                    target = ASINs.get(targetId);
                }

                selectedASINs.add(target);

                int unit = (int) (Math.random() * 10 + 1);

                units.put(target, unit);
            }
            int hour = (int) (Math.random() * 23 + 1);

            Order order = new Order(units, "" + i, option, x, y, hour);
            orders.add(order);
            idOrders.put(order.id, order);

            if (!hourOrders.containsKey(hour)) {
                hourOrders.put(hour, new LinkedList<>());
            }
            hourOrders.get(hour).add(order);

            sortedShippingDistances.put(order, sorted);

        }

    }

    void buildFCs(int size) {
        for (int i = 0; i < size; i++) {
            String id = "" + i;
            double x = Math.random() * max_X;
            double y = Math.random() * max_Y;
            double unitProcessingCost = 0.55;
            double packagingCost = 0.43;
            double unitStorageCost = 0.05;
            int storageCapacity = 100;
            FC fc = new FC(id, x, y, sortable, unitProcessingCost, packagingCost, unitStorageCost,
                    storageCapacity);
            FCs.add(fc);
        }
    }

    int getCurrentStorage(FC fc) {
        int storage = 0;
        for (ASIN asin : fc.inventories.keySet()) {
            storage += fc.inventories.get(asin);
        }
        return storage;
    }

    void updateInventory(FC fc, Order order) {
        if (!shipments.containsKey(order))
            shipments.put(order, new ArrayList<>());

        Map<ASIN, Integer> packageUnits = new HashMap<>();

        for (ASIN asin : order.units.keySet()) {
            int curInv;
            if (fc.inventories.containsKey(asin)) {
                curInv = fc.inventories.get(asin);
            } else
                curInv = 0;
            curInv += order.units.get(asin);
            fc.inventories.put(asin, curInv);

            packageUnits.put(asin, order.units.get(asin));
        }

        Shipment shipment = new Shipment(order.id, order.id + "-1", packageUnits, fc);
        shipments.get(order).add(shipment);
//        packages.put(order, pack);


    }

    boolean isFCFulfillable(FC fc, Order order) {
        int currentStorage = getCurrentStorage(fc);
        for (ASIN asin : order.units.keySet()) {
            currentStorage += order.units.get(asin);
            if (currentStorage > fc.storageCapacity) {
                return false;
            }
        }
        return true;
    }

    double getTotalWeight(Map<ASIN, Integer> units) {
        double weight = 0;
        for (ASIN asin : units.keySet()) {
            weight += asin.weight * units.get(asin);
        }
        return weight;
    }

    double getTotalUnits(Map<ASIN, Integer> units) {
        double total = 0;
        for (ASIN asin : units.keySet()) {
            total += units.get(asin);
        }
        return total;
    }

    List<Map<ASIN, Integer>> findASINUnitsCombos(Shipment shipment) {
        List<Map<ASIN, Integer>> combination = new ArrayList<>();

        List<Set<ASIN>> ASINcombos = new ArrayList<>();
        List<ASIN> ASINset = new ArrayList<>();
        ASINset.addAll(shipment.units.keySet());
        for (int i = 1; i < ASINset.size(); i++) {
            List<Set<ASIN>> combos = Utils.getSubsets(ASINset, i);
            ASINcombos.addAll(combos);
        }

        for (Set<ASIN> element : ASINcombos) {
            Map<ASIN, Integer> ASINunits = new HashMap<>();
            for (ASIN asin : element) {
                ASINunits.put(asin, shipment.units.get(asin));
            }
            combination.add(ASINunits);
        }
        return combination;
    }

    Map<ASIN, Integer> splitShipment(Shipment shipment, Map<ASIN, Integer> split) {
        Map<ASIN, Integer> afterSplit = new HashMap<>();
        for (ASIN asin : shipment.units.keySet()) {
            if (split.containsKey(asin))
                continue;
            afterSplit.put(asin, shipment.units.get(asin));
        }
        return afterSplit;
    }

    void neighborhoodSearchwithSplit() {
        Map<SplitContext, Double> splitScores = new HashMap<>();
        for (Order order : shipments.keySet()) {
            for (Shipment shipment : shipments.get(order)) {
                //for each combination of ASINs, find a set of feasible FCs, then generate splitContext
                List<Map<ASIN, Integer>> asinComboUnits = findASINUnitsCombos(shipment);
                for (Map<ASIN, Integer> splitUnits : asinComboUnits) {
                    Map<ASIN, Integer> afterSplit = splitShipment(shipment, splitUnits);
                    for (FC fc : sortedShippingDistances.get(order).keySet()) {
                        if (!fc.equals(shipment.fc)) {
                            if (fc.getTotalInventory() + getTotalUnits(splitUnits) > fc.storageCapacity)
                                continue;
                            double originalCost = 0;
                            originalCost += ShippingCost.cost(getTotalWeight(shipment.units), sortedShippingDistances.get(order).get(fc));
                            originalCost += shipment.fc.packagingCost;
                            originalCost += getTotalUnits(shipment.units) * shipment.fc.unitStorageCost;
                            originalCost += getTotalUnits(shipment.units) * shipment.fc.unitProcessingCost;

                            double newCost = 0;
                            newCost += ShippingCost.cost(getTotalWeight(splitUnits), sortedShippingDistances.get(order).get(fc));
                            newCost += fc.packagingCost;
                            newCost += getTotalUnits(splitUnits) * fc.unitStorageCost;
                            newCost += getTotalUnits(splitUnits) * fc.unitProcessingCost;

                            newCost += ShippingCost.cost(getTotalWeight(afterSplit), sortedShippingDistances.get(order).get(fc));
                            newCost += shipment.fc.packagingCost;
                            newCost += getTotalUnits(afterSplit) * shipment.fc.unitStorageCost;
                            newCost += getTotalUnits(afterSplit) * shipment.fc.unitProcessingCost;

                            double score = originalCost - newCost;
                            if (score <= 0)
                                continue;

                            SplitContext context = new SplitContext(shipment, splitUnits, fc);
                            splitScores.put(context, score);
                        }
                    }
                }
            }
        }

        if (splitScores.size() == 0) {
            System.out.println("No potential cost improvement found by changing fulfillment FC for the split context...");
            return;
        }

        Map<SplitContext, Double> sortedSplitScores = Utils.sortByValue(splitScores);
        SplitContext targetSplitContext = (SplitContext) sortedSplitScores.keySet().toArray()[0];

        //build a new shipment for split content
        Shipment splitShipment = new Shipment(targetSplitContext.originalShipment.orderId, targetSplitContext.originalShipment.shipmentId + "_Split", targetSplitContext.content,
                targetSplitContext.newFC);
        shipments.get(idOrders.get(targetSplitContext.originalShipment.orderId)).add(splitShipment);
        //update target FC inventory
        for (ASIN asin : targetSplitContext.content.keySet()) {
            if (!targetSplitContext.newFC.inventories.containsKey(asin))
                targetSplitContext.newFC.inventories.put(asin, 0);
            int current = targetSplitContext.newFC.inventories.get(asin);
            current += targetSplitContext.content.get(asin);
            targetSplitContext.newFC.inventories.put(asin, current);
        }
        //update orignal FC inventory
        for(ASIN asin : targetSplitContext.content.keySet()){
            targetSplitContext.originalShipment.units.remove(asin);

            int current = targetSplitContext.originalShipment.fc.inventories.get(asin);
            current -= targetSplitContext.originalShipment.units.get(asin);
            targetSplitContext.originalShipment.fc.inventories.put(asin, current);
        }
    }

    void neighborhoodSearchwithoutSplit() {
        //order neighborhood search
        Map<Shipment, Double> shipmentScores = new HashMap<>();
        Map<Shipment, FC> shipmentTargetFCs = new HashMap<>();
        for (Order order : shipments.keySet()) {
            for (Shipment shipment : shipments.get(order)) {
                for (FC fc : sortedShippingDistances.get(order).keySet()) {
                    if (!fc.equals(shipment.fc)) {
                        if (fc.getTotalInventory() + getTotalUnits(shipment.units) > fc.storageCapacity)
                            continue;

                        double fcShippingCost = ShippingCost.cost(getTotalWeight(shipment.units), sortedShippingDistances.get(order).get(fc));
                        double currentShippingCost = ShippingCost.cost(getTotalWeight(shipment.units), sortedShippingDistances.get(order).get(shipment.fc));
                        shipment.score = currentShippingCost - fcShippingCost;

                        shipment.score += shipment.fc.packagingCost - fc.packagingCost;

                        shipment.score += getTotalUnits(shipment.units) * (shipment.fc.unitStorageCost - fc.unitStorageCost);
                        shipment.score += getTotalUnits(shipment.units) * (shipment.fc.unitProcessingCost - fc.unitProcessingCost);

                        if (shipment.score <= 0)
                            continue;

                        shipmentScores.put(shipment, shipment.score);
                        shipmentTargetFCs.put(shipment, fc);
//                        break;
                    }
                }
            }
        }

        if (shipmentScores.size() == 0) {
            System.out.println("No potential cost improvement found by changing fulfillment FC for the whole package...");
            return;
        }

        Map<Shipment, Double> sortedShipmentScores = Utils.sortByValue(shipmentScores);
        Shipment targetShipment = (Shipment) sortedShipmentScores.keySet().toArray()[0];
        FC targetFC = shipmentTargetFCs.get(targetShipment);

        //update target FC inventory
        for (ASIN asin : targetShipment.units.keySet()) {
            if (!targetFC.inventories.containsKey(asin))
                targetFC.inventories.put(asin, 0);
            int current = targetFC.inventories.get(asin);
            current += targetShipment.units.get(asin);
            targetFC.inventories.put(asin, current);
        }
        //update orignal FC inventory
        for (ASIN asin : targetShipment.units.keySet()) {
            int current = targetShipment.fc.inventories.get(asin);
            current -= targetShipment.units.get(asin);
            targetShipment.fc.inventories.put(asin, current);
        }

        //update shipment information
        targetShipment.fc = targetFC;
    }

    void solveByHeuristic() {
        //For each hour, consolidate demand of each ASIN
        //orders are not split
        for (int i = 0; i < 24; i++) {
            if (!hourOrders.containsKey(i))
                continue;
            for (Order order : hourOrders.get(i)) {
                for (FC fc : sortedShippingDistances.get(order).keySet()) {
                    if (isFCFulfillable(fc, order)) {
                        updateInventory(fc, order);
                        break;
                    }
                }
            }
        }

        int step = 0;
        while (step <= 9) {
            step++;
            System.out.println("### Step " + step);
            neighborhoodSearchwithoutSplit();
        }


        //ASIN neighborhood search


        //target on the FCs whose inventory violate capacity
        //For each target FC, target on the orders which are assigned to the FC.
        //For each order, compute a cost if assign it to the second least shipping cost FC. Compute a score based on the cost difference and the increasing storaging cost of second
        //least cost FC
        //Repeat above steps until all FCs do not violate storage capacity

        //Find the most promising move which can decrease the total cost
        // -- if no_spit first, target on those orders which are split
        // For the target order,
        // -- if total cost first, target on order has largest cost (shipping cost + packaging cost + unit processing cost - storage cost)
        // For the target order, find another fulfillment way. If no other way is found, continue to next order

    }

//    void buildModel() {
//        long startBuildingModel = System.currentTimeMillis();
//        //variable definition
//        System.out.println("Start building variables...");
//        Map<FC, Map<ASIN, XPRBvar>> inventoryVars = new LinkedHashMap<>();
//        Map<FC, Map<Order, XPRBvar>> FCOrderVars = new LinkedHashMap<>();
//        Map<Order, Map<FC, Map<ASIN, XPRBvar>>> orderFCASINVars = new LinkedHashMap<>();
//        for (Order order : orders) {
//            if (!orderFCASINVars.containsKey(order)) {
//                orderFCASINVars.put(order, new LinkedHashMap<>());
//            }
//
//            for (FC fc : FCs) {
//                double thresholdDistance = (double) sortedShippingDistances.get(order).values().toArray()[FIRST_K_CLOSEST];
//                if (sortedShippingDistances.get(order).get(fc) > thresholdDistance) {
//                    continue;
//                }
//
//                if (!FCOrderVars.containsKey(fc)) {
//                    FCOrderVars.put(fc, new LinkedHashMap<>());
//                }
//
//                if (!FCOrderVars.get(fc).containsKey(order)) {
//                    XPRBvar fo = problem.newVar("OF" + "_" + fc.id + "_" + order.id, XPRB.BV);
//                    FCOrderVars.get(fc).put(order, fo);
//                }
//
//                if (!orderFCASINVars.get(order).containsKey(fc)) {
//                    orderFCASINVars.get(order).put(fc, new LinkedHashMap<>());
//                }
//
//                for (ASIN asin : order.units.keySet()) {
//                    XPRBvar ofa = problem.newVar("AF" + "_" + fc.id + "_" + order.id + "_" + asin.id,
//                            XPRB.UI, 0, 100);
//                    orderFCASINVars.get(order).get(fc).put(asin, ofa);
//
//                    if (!inventoryVars.containsKey(fc)) {
//                        inventoryVars.put(fc, new LinkedHashMap<>());
//                    }
//
//
//                    if (!inventoryVars.get(fc).containsKey(asin)) {
//                        XPRBvar inv = problem.newVar("IN" + "_" + fc.id + "_" + asin.id, XPRB.UI, 0, 10000);
//                        inventoryVars.get(fc).put(asin, inv);
//                    }
//                }
//            }
//        }
//
//        System.out.println("Building variables complete");
//        System.out.println("Start building objective...");
//        //objective
//        XPRBexpr obj = new XPRBexpr();
//
//        System.out.println("  Building shipping cost");
//        for (Order order : orderFCASINVars.keySet()) {
//            if (Integer.valueOf(order.id) % 100 == 0)
//                System.out.println("    Handling Order " + order.id);
//            for (FC fc : orderFCASINVars.get(order).keySet()) {
//                double shippingDistance = ShippingCost.EclidDistance(fc.x, fc.y, order.x, order.y) * UNIT_DISTANCE_COST;
//                for (ASIN asin : orderFCASINVars.get(order).get(fc).keySet()) {
//                    obj.add(orderFCASINVars.get(order).get(fc).get(asin).mul(asin.weight * ALPHA * shippingDistance).add(BETA));
//                }
//            }
//        }
//        System.out.println("  Building shipping cost complete");
//
//        System.out.println("  Building unit processing cost");
//        for (Order order : orderFCASINVars.keySet()) {
//            if (Integer.valueOf(order.id) % 100 == 0)
//                System.out.println("    Handling Order " + order.id);
//            for (FC fc : orderFCASINVars.get(order).keySet()) {
//                for (ASIN asin : orderFCASINVars.get(order).get(fc).keySet()) {
//                    obj.add(orderFCASINVars.get(order).get(fc).get(asin).mul(fc.unitProcessingCost));
//                }
//            }
//        }
//        System.out.println("  Building unit processing cost complete");
//
//        System.out.println("  Building packaging cost");
//        for (FC fc : FCs) {
//            if (!FCOrderVars.containsKey(fc))
//                continue;
//            System.out.println("  Handling FC " + fc.id);
//            for (Order order : FCOrderVars.get(fc).keySet()) {
//                obj.add(FCOrderVars.get(fc).get(order).mul(fc.packagingCost));
//            }
//        }
//        System.out.println("  Building packaging cost complete");
//
//        System.out.println("  Building storage cost");
//        for (FC fc : FCs) {
//            if (!inventoryVars.containsKey(fc))
//                continue;
//            System.out.println("    Handling FC " + fc.id);
//            for (ASIN asin : inventoryVars.get(fc).keySet()) {
//                obj.add(inventoryVars.get(fc).get(asin).mul(fc.unitStorageCost));
//            }
//        }
//        System.out.println("  Building storage cost complete");
//
//        problem.setObj(obj);
//        System.out.println("Building objective complete");
//        problem.setSense(XPRB.MINIM);
//
//
//        //storage capacity constraint
//        System.out.println("Start building storage capacity constraint...");
//        for (FC fc : FCs) {
//            System.out.println("  Handling FC " + fc.id);
//            if (!inventoryVars.containsKey(fc))
//                continue;
//            XPRBexpr ctr = new XPRBexpr();
//            for (ASIN asin : inventoryVars.get(fc).keySet()) {
//                ctr.add(inventoryVars.get(fc).get(asin).mul(1));
//            }
//            problem.newCtr(fc.id + "_StorageCap", ctr.lEql(fc.storageCapacity));
//        }
//        System.out.println("Building storage capacity constraint complete");
//        //inventory available constraint
//        System.out.println("Start building inventory available constraint...");
//        for (FC fc : FCs) {
//            System.out.println("  Handling FC " + fc.id);
//            if (!inventoryVars.containsKey(fc))
//                continue;
//            for (ASIN asin : inventoryVars.get(fc).keySet()) {
//                XPRBexpr ctr = new XPRBexpr();
//                ctr.add(inventoryVars.get(fc).get(asin).mul(-1));
//
//                for (Order order : orderFCASINVars.keySet()) {
//                    if (!orderFCASINVars.get(order).containsKey(fc))
//                        continue;
//                    if (!orderFCASINVars.get(order).get(fc).containsKey(asin))
//                        continue;
//                    ctr.add(orderFCASINVars.get(order).get(fc).get(asin).mul(1));
//                }
//                problem.newCtr(fc.id + "_" + asin.id + "_InventoryAvailable", ctr.lEql(0));
//            }
//        }
//        System.out.println("Building inventory available constraint complete");
//
//        //order fulfillment constraint
//        System.out.println("Start building order fulfillment constraint...");
//        for (Order order : orders) {
//            if (Integer.valueOf(order.id) % 100 == 0)
//                System.out.println("  Handling Order " + order.id);
//            for (ASIN asin : order.units.keySet()) {
//                XPRBexpr ctr = new XPRBexpr();
//                for (FC fc : FCs) {
//                    if (!orderFCASINVars.get(order).containsKey(fc))
//                        continue;
//                    ctr.add(orderFCASINVars.get(order).get(fc).get(asin).mul(1));
//                }
//                problem.newCtr("OF" + order.id + "_" + asin.id, ctr.eql(order.units.get(asin)));
//            }
//        }
//        System.out.println("Building order fulfillment constraint complete");
//
//        //order fulfillment compatibility constraint
//        System.out.println("Start building /order fulfillment compatibility constraint...");
//        for (Order order : orders) {
//            if (Integer.valueOf(order.id) % 100 == 0)
//                System.out.println("  Handling Order " + order.id);
//            for (ASIN asin : order.units.keySet()) {
//                for (FC fc : FCs) {
//                    if (!orderFCASINVars.get(order).containsKey(fc))
//                        continue;
//                    if (!FCOrderVars.get(fc).containsKey(order))
//                        continue;
//                    XPRBexpr ctr = new XPRBexpr();
//                    ctr.add(orderFCASINVars.get(order).get(fc).get(asin).mul(1));
//                    ctr.add(FCOrderVars.get(fc).get(order).mul(-LARGE_POSITIVE));
//                    problem.newCtr("Compatible_" + order.id + "_" + asin.id + "_" + fc.id, ctr.lEql(0));
//                }
//            }
//        }
//        System.out.println("Building order fulfillment compatibility complete");
//
//        System.out.println("Building model time " + (System.currentTimeMillis() - startBuildingModel));
//
//        long startSolving = System.currentTimeMillis();
//        problem.mipOptimise();
//        System.out.println("Solving time " + (System.currentTimeMillis() - startSolving));
//        if (problem.getMIPStat() == XPRB.MIP_INFEAS) {
//            System.out.println("MIP Infeasible!");
//        } else if (problem.getMIPStat() == XPRB.MIP_OPTIMAL) {
//            System.out.println("Objective: " + problem.getObjVal());
//            System.out.println("------Inventory------");
//            for (FC fc : inventoryVars.keySet()) {
//                for (ASIN asin : inventoryVars.get(fc).keySet()) {
//                    if (Math.abs(inventoryVars.get(fc).get(asin).getSol()) <= INTEGER_GAP)
//                        continue;
//                    System.out.println("++FC" + fc.id + "  " + asin.id + "  " + inventoryVars.get(fc).get(asin).getSol());
//                }
//            }
//            System.out.println();
//            System.out.println("------------Order Fulfillment--------");
//            for (Order order : orders) {
//                System.out.println("++Order " + order.id);
//                for (FC fc : FCs) {
//                    if (!orderFCASINVars.get(order).containsKey(fc))
//                        continue;
//                    for (ASIN asin : order.units.keySet()) {
//                        if (Math.abs(orderFCASINVars.get(order).get(fc).get(asin).getSol()) <= INTEGER_GAP)
//                            continue;
//                        System.out.println("   FC " + fc.id + "     ASIN " + asin.id + "   " + orderFCASINVars.get(order).get(fc).get(asin).getSol());
//                    }
//                }
//            }
//        } else {
//            System.out.println(problem.getMIPStat());
//        }
//
//    }
}
