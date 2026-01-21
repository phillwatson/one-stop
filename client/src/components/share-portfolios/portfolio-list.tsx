import { useCallback, useEffect, useState } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import IconButton from '@mui/material/IconButton';
import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { SxProps } from '@mui/material/styles';

import { useMessageDispatch } from '../../contexts/messages/context';
import { PortfolioSummaryResponse } from '../../model/share-portfolio.model';
import PortfolioService from '../../services/portfolio.service';
import { toLocaleDate } from '../../util/date-util';

interface Props {
  /**
   * Callback function that receives the selected portfolio.
   * @param portfolio The selected portfolio, or undefined if deselected.
   */
  onSelectPortfolio?: (portfolio: PortfolioSummaryResponse | undefined) => void;
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

const selectableRow: SxProps = {
  cursor: 'pointer',
  '&:hover': {
    backgroundColor: '#f5f5f5'
  }
};

interface CreatePortfolioDialogProps {
  open: boolean;
  portfolioName: string;
  onNameChange: (name: string) => void;
  onCancel: () => void;
  onConfirm: () => void;
  isCreating: boolean;
}

interface DeletePortfolioDialogProps {
  open: boolean;
  portfolio: PortfolioSummaryResponse | undefined;
  confirmationText: string;
  onConfirmationTextChange: (text: string) => void;
  onCancel: () => void;
  onConfirm: () => void;
  isDeleting: boolean;
}

function CreatePortfolioDialog(props: CreatePortfolioDialogProps) {
  function validateForm(): boolean {
    return props.portfolioName.trim().length > 0;
  }

  return (
    <Dialog open={props.open} onClose={props.onCancel} fullWidth>
      <DialogTitle sx={{ bgcolor: 'primary.main', color: 'white' }}>Create New Portfolio</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 2 }}>
          <TextField
            id="portfolioName"
            label="Portfolio Name"
            autoFocus
            required
            fullWidth
            variant="standard"
            value={props.portfolioName}
            onChange={(e) => props.onNameChange(e.target.value)}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={props.onCancel} variant="outlined">Cancel</Button>
        <Button
          onClick={props.onConfirm}
          variant="contained"
          disabled={!validateForm() || props.isCreating}
        >
          Create
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function DeletePortfolioDialog(props: DeletePortfolioDialogProps) {
  function validateForm(): boolean {
    return props.confirmationText === props.portfolio?.name;
  }

  return (
    <Dialog open={props.open} onClose={props.onCancel} fullWidth>
      <DialogTitle sx={{ bgcolor: 'primary.main', color: 'white' }}>Delete Portfolio</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 2 }}>
          <p>To confirm deletion of "<strong>{props.portfolio?.name}</strong>", please enter the portfolio name:</p>
          <TextField
            id="confirmationName"
            label="Portfolio Name"
            autoFocus
            required
            fullWidth
            variant="standard"
            value={props.confirmationText}
            onChange={(e) => props.onConfirmationTextChange(e.target.value)}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={props.onCancel} variant="outlined">Cancel</Button>
        <Button
          onClick={props.onConfirm}
          variant="contained"
          color="error"
          disabled={!validateForm() || props.isDeleting}
        >
          Delete
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/**
 * A component to retrieve portfolios and display them in a list.
 * The selected portfolio can be passed to a parent component via a callback.
 */
export default function PortfolioList(props: Props) {
  const showMessage = useMessageDispatch();
  const [portfolios, setPortfolios] = useState<PortfolioSummaryResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<string | undefined>();

  const [createDialogOpen, setCreateDialogOpen] = useState<boolean>(false);
  const [newPortfolioName, setNewPortfolioName] = useState<string>('');
  const [creating, setCreating] = useState<boolean>(false);

  const [deleteDialogOpen, setDeleteDialogOpen] = useState<boolean>(false);
  const [portfolioToDelete, setPortfolioToDelete] = useState<PortfolioSummaryResponse | undefined>();
  const [deleteConfirmationText, setDeleteConfirmationText] = useState<string>('');
  const [deleting, setDeleting] = useState<boolean>(false);

  const refreshPortfolios = useCallback(() => {
    setLoading(true);
    PortfolioService.getPortfolios()
      .then(response => setPortfolios(response.items || []))
      .catch(err => showMessage(err))
      .finally(() => setLoading(false));
  }, [ showMessage]);

  useEffect(() => {
    refreshPortfolios();
  }, [ refreshPortfolios ]);

  function handleSelectPortfolio(portfolio?: PortfolioSummaryResponse) {
    const newSelectedId = portfolio?.id;
    setSelectedPortfolioId(newSelectedId);
    
    if (props.onSelectPortfolio) {
      props.onSelectPortfolio(portfolio);
    }
  }

  function handleOpenCreateDialog() {
    setNewPortfolioName('');
    setCreateDialogOpen(true);
  }

  function handleCloseCreateDialog() {
    setCreateDialogOpen(false);
    setNewPortfolioName('');
  }

  function handleCreatePortfolio() {
    setCreating(true);
    PortfolioService.createPortfolio(newPortfolioName)
      .then(() => {
        showMessage({ type: 'add', level: 'success', text: `Portfolio "${newPortfolioName}" created successfully` });
        handleCloseCreateDialog();
        refreshPortfolios();
      })
      .catch(err => showMessage(err))
      .finally(() => setCreating(false));
  }

  function handleOpenDeleteDialog(portfolio: PortfolioSummaryResponse, event: React.MouseEvent) {
    event.stopPropagation();
    setPortfolioToDelete(portfolio);
    setDeleteConfirmationText('');
    setDeleteDialogOpen(true);
  }

  function handleCloseDeleteDialog() {
    setDeleteDialogOpen(false);
    setPortfolioToDelete(undefined);
    setDeleteConfirmationText('');
  }

  function handleDeletePortfolio() {
    if (!portfolioToDelete) return;

    setDeleting(true);
    PortfolioService.deletePortfolio(portfolioToDelete.id)
      .then(() => {
        showMessage({ type: 'add', level: 'success', text: `Portfolio "${portfolioToDelete.name}" deleted successfully` });
        if (selectedPortfolioId === portfolioToDelete.id) {
          handleSelectPortfolio(undefined);
        }
        handleCloseDeleteDialog();
        refreshPortfolios();
      })
      .catch(err => showMessage(err))
      .finally(() => setDeleting(false));
  }

  if (loading) {
    return <div>Loading portfolios...</div>;
  }

  return (
    <Box>
      { (portfolios.length === 0) &&
        <Typography sx={{ typography: { xs: "h6", sm: "h5" } }} noWrap align='center'>
          No portfolios found
        </Typography>
      }

      <Box sx={{ mb: 2 }}>
        <Button variant="contained" onClick={ handleOpenCreateDialog } sx={{ mt: 2 }} >
          Create Portfolio
        </Button>
      </Box>

      { (portfolios.length > 0) &&
        <TableContainer>
          <Table size='small'>
            <TableHead>
              <TableRow>
                <TableCell sx={colhead} align='left'>Name</TableCell>
                <TableCell sx={colhead} align='center'>Created</TableCell>
                <TableCell sx={colhead} align='center' width="40px"></TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {portfolios
                .sort((a, b) => a.name.localeCompare(b.name))
                .map(portfolio => (
                  <TableRow key={portfolio.id} sx={selectableRow}
                    onClick={() => handleSelectPortfolio(portfolio)}
                    selected={selectedPortfolioId === portfolio.id}
                  >
                    <TableCell align='left'>{portfolio.name}</TableCell>
                    <TableCell align='center'>
                      { toLocaleDate(portfolio.dateCreated) }
                    </TableCell>
                    <TableCell align='center' width="40px">
                      <Tooltip title="Delete portfolio">
                        <IconButton size="small" onClick={(e) => handleOpenDeleteDialog(portfolio, e)} >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              }
            </TableBody>
          </Table>
        </TableContainer>
      }

      <CreatePortfolioDialog
        open={createDialogOpen}
        portfolioName={newPortfolioName}
        onNameChange={setNewPortfolioName}
        onCancel={handleCloseCreateDialog}
        onConfirm={handleCreatePortfolio}
        isCreating={creating}
      />

      <DeletePortfolioDialog
        open={deleteDialogOpen}
        portfolio={portfolioToDelete}
        confirmationText={deleteConfirmationText}
        onConfirmationTextChange={setDeleteConfirmationText}
        onCancel={handleCloseDeleteDialog}
        onConfirm={handleDeletePortfolio}
        isDeleting={deleting}
      />
    </Box>
  );
}