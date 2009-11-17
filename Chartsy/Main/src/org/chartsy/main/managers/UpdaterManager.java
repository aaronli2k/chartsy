package org.chartsy.main.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;
import org.chartsy.main.RestoreSettings;
import org.chartsy.main.dataset.Dataset;
import org.chartsy.main.updater.AbstractUpdater;
import org.chartsy.main.utils.Stock;
import org.chartsy.main.utils.XMLUtils;
import org.openide.util.Lookup;

/**
 *
 * @author viorel.gheba
 */
public class UpdaterManager {

    protected static UpdaterManager instance;
    protected Hashtable<Object, Object> updaters;
    protected AbstractUpdater active;
    protected boolean update = false;

    public static UpdaterManager getDefault() {
        if (instance == null) instance = new UpdaterManager();
        return instance;
    }

    protected UpdaterManager() {}

    public void initialize() {
        updaters = new Hashtable<Object, Object>();
        Collection<? extends AbstractUpdater> list = Lookup.getDefault().lookupAll(AbstractUpdater.class);
        for (AbstractUpdater au : list) {
            addUpdater(au.getName(), au);
        }
    }

    public void addUpdater(Object key, Object value) { updaters.put(key, value); }
    public void removeUpdater(Object key) { updaters.remove(key); }

    public AbstractUpdater getUpdater(Object key) {
        Object obj = updaters.get(key);
        if (obj != null && obj instanceof AbstractUpdater) return (AbstractUpdater) obj;
        return null;
    }

    public void setActiveUpdater(Object key) {
        Object obj = updaters.get(key);
        if (obj != null && obj instanceof AbstractUpdater) {
            AbstractUpdater inactive = (AbstractUpdater) obj;
            if (active == null) {
                active = inactive;
                XMLUtils.setActiveDataProvider(active.getName());
            } else {
                String activeName = active.getName();
                String inactiveName = inactive.getName();
                if (!activeName.equals(inactiveName)) {
                    fireUpdaterChange(inactive);
                }
            }
        }
    }
    public AbstractUpdater getActiveUpdater() { return active; }
    public String getActiveUpdaterName() { return active != null ? active.getName() : null; }

    public Vector getUpdaters() {
        Vector v = new Vector();
        Iterator it = updaters.keySet().iterator();
        while (it.hasNext()) v.add(it.next());
        Collections.sort(v);
        return v;
    }

    public void update(Stock stock) {
        Dataset dataset;
        if (active != null) {
            for (String time : DatasetManager.LIST) {
                dataset = active.update(stock.getKey(), time);
                if (dataset != null) DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, time), dataset);
            }
        }
        setUpdate(true);
    }

    public void update(Stock[] stocks) {
        Dataset dataset;
        if (active != null) {
            for (Stock stock : stocks) {
                for (String time : DatasetManager.LIST) {
                    dataset = active.update(stock.getKey(), time);
                    if (dataset != null) DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, time), dataset);
                }
            }
        }
        setUpdate(true);
    }

    public void update(LinkedHashMap stocks) {
        Dataset dataset;
        if (active != null) {
            Iterator it = stocks.keySet().iterator();
            while (it.hasNext()) {
                Stock stock = (Stock) it.next();
                String time = (String) stocks.get(stock);
                if (!time.contains("Min")) {
                    for (String t : DatasetManager.LIST) {
                        dataset = active.update(stock.getKey(), t);
                        if (dataset != null) DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, t), dataset);
                    }
                } else {
                    for (String t : DatasetManager.LIST) {
                        dataset = active.update(stock.getKey(), t);
                        if (dataset != null) DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, t), dataset);
                    }
                    dataset = active.updateIntraDay(stock.getKey(), time);
                    if (dataset != null) DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, time), dataset);
                }
            }
        }
        setUpdate(true);
    }

    public void update(Stock stock, String time) {
        Dataset dataset;
        if (active != null) {
            dataset = (!time.contains("Min")) ? active.update(stock.getKey(), time) : active.updateIntraDay(stock.getKey(), time);
            if (dataset != null) DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, time), dataset);
        }
        setUpdate(true);
    }

    public void updateIntraDay(Stock stock, String time) {
        Dataset dataset;
        if (active != null) {
            dataset = active.updateIntraDay(stock.getKey(), time);
            if (dataset != null) DatasetManager.getDefault().addDataset(DatasetManager.getName(stock, time), dataset);
        }
        setUpdate(true);
    }

    public void setUpdate(boolean b) { update = b; }
    public boolean isUpdated() { return update; }

    public void print() {
        Iterator it = updaters.keySet().iterator();
        while (it.hasNext()) System.out.println(it.next().toString());
    }

    public void fireUpdaterChange(AbstractUpdater inactive) {
        ChartFrameManager.getDefault().saveAll(); // save settings
        ChartFrameManager.getDefault().closeAll(); // close all chart frames
        DatasetManager.getDefault().removeAll(); // remove datasets
        active = inactive;
        XMLUtils.setActiveDataProvider(active.getName());
        RestoreSettings.newInstance().restore();
    }

}
