import { useState, useEffect, useMemo } from 'react';

import Paper from '@mui/material/Paper';
import { Accordion, AccordionDetails, AccordionSummary, Typography } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

import { useMessageDispatch } from '../../contexts/messages/context';
import AuditReportService from "../../services/audit-report.service";
import { AuditIssueSummary, AuditReportConfig } from '../../model/audit-report.model';
import AuditIssuesList from './audit-issues-list';

interface Props {
}

export default function AuditReportResults(props: Props) {
  const showMessage = useMessageDispatch();

  const [ issueSummaries, setIssueSummaries ] = useState<Array<AuditIssueSummary>>([]);
  const [ reportConfigs, setReportConfigs ] = useState<Array<AuditReportConfig>>([]);
  useEffect(() => {
    AuditReportService.getAuditIssueSummaries()
      .then(response => setIssueSummaries(response))
      .then(() => { AuditReportService.fetchAllConfigs()
        .then(response => setReportConfigs(response));
      })
      .catch(err => showMessage(err));
  }, [ showMessage ]);

  const [ selectedReportConfigId, setSelectedReportConfigId ] = useState<string>();
  function selectReport(configId?: string) {
    if (configId === selectedReportConfigId) {
      setSelectedReportConfigId(undefined);
    } else {
      setSelectedReportConfigId(configId)
    }
  }

  const selectedReportConfig = useMemo(() => {
    if (selectedReportConfigId) {
      return reportConfigs.find(r => r.id === selectedReportConfigId);
    } else {
      return undefined;
    }
  }, [ selectedReportConfigId, reportConfigs ]);

  return (
    <>
      { issueSummaries.map(summary => (
        <Accordion key={ summary.auditConfigId }
          expanded={ selectedReportConfigId === summary.auditConfigId }
          onChange={ () => selectReport(summary.auditConfigId) }>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}  >
            <Typography > { summary.auditConfigName } </Typography>
            <Typography sx={{ color: 'text.secondary' }}>&nbsp;( { summary.totalCount } )</Typography>
          </AccordionSummary>
          <AccordionDetails>
            { selectedReportConfigId && selectedReportConfig &&
              <>
                <Typography variant='caption' >{ selectedReportConfig.description }</Typography>
                <Paper elevation={ 2 } sx={{ padding: 1}}>
                  <AuditIssuesList reportConfig={ selectedReportConfig } />
                </Paper>
              </>
            }
            </AccordionDetails>
        </Accordion>
      ))}
    </>
  );
}