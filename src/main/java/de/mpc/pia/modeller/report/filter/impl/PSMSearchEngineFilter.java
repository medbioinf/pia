package de.mpc.pia.modeller.report.filter.impl;

import de.mpc.pia.modeller.psm.PSMReportItem;
import de.mpc.pia.modeller.psm.ReportPSM;
import de.mpc.pia.modeller.psm.ReportPSMSet;
import de.mpc.pia.modeller.report.filter.AbstractFilter;
import de.mpc.pia.modeller.report.filter.FilterComparator;
import de.mpc.pia.modeller.report.filter.RegisteredFilters;
import de.mpc.pia.modeller.score.ScoreModel;
import de.mpc.pia.modeller.score.ScoreModelEnum;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This code is licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * ==Overview==
 *
 * @author ypriverol on 19/10/2018.
 */

public class PSMSearchEngineFilter extends AbstractFilter {

    private static final long serialVersionUID = -7592882824717030348L;
    private static final Logger log = Logger.getLogger(PSMSearchEngineFilter.class);

    public PSMSearchEngineFilter(FilterComparator arg, boolean negate) {
        super(arg, RegisteredFilters.SEARCH_ENGINE_PSM_FILTER, negate);
    }


    @Override
    public Object getFilterValue() {
        return "";
    }

    @Override
    public boolean supportsClass(Object c) {
        boolean supports = false;

        if (c instanceof PSMReportItem) {
            // it also depends on the score ("average FDR score" and "combined fdr score" is PSMSet, all other are PSM)
            if (c instanceof ReportPSM) {
                log.debug(((ReportPSM) c).getSpectrum().getScores().toString());
                ReportPSM psm = ((ReportPSM) c);
                Optional<ScoreModel> nonMatch = psm.getSpectrum().getScores().stream().filter(x -> x.getValue() == 0.0).findAny();
                if(!nonMatch.isPresent())
                    supports = true;

            } else if (c instanceof ReportPSMSet) {
                ReportPSMSet psmSet = (ReportPSMSet) c;
                log.debug(psmSet.toString());
                Map<String, List<ScoreModel>> scores = psmSet.getPSMs()
                        .stream().parallel().map(sp -> sp.getSpectrum().getScores())
                        .flatMap(Collection::stream)
                        .filter(y -> !ScoreModelEnum.nonNativeScoreModels.contains(y.getType()))
                        .collect(Collectors.toList()).stream().collect(Collectors.groupingBy(ScoreModel::getAccession));
                boolean all = true;
                for(Map.Entry score: scores.entrySet()){
                    List<ScoreModel> keyValue = (List<ScoreModel>) score.getValue();
                    Optional<ScoreModel> value = keyValue.stream().filter(x -> x.getValue() != 0.0).findAny();
                    if(!value.isPresent())
                        all = false;
                }
                if(all)
                    supports = true;
            }
        }

        return supports;
    }

    @Override
    public Object getObjectsValue(Object obj) {
        if (obj instanceof ReportPSMSet) {
            ReportPSMSet psmSet = (ReportPSMSet) obj;
            return psmSet.getPSMs()
                    .stream().parallel().map(sp -> sp.getSpectrum().getScores())
                    .flatMap(Collection::stream)
                    .filter(y -> !ScoreModelEnum.nonNativeScoreModels.contains(y.getType()))
                    .collect(Collectors.toList()).stream().collect(Collectors.groupingBy(ScoreModel::getAccession));
        } else if (obj instanceof Number) {
            return obj;
        }

        // nothing supported
        return null;
    }
}
