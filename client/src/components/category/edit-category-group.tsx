import { useEffect, useState } from "react";
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from "@mui/material";

import { CategoryGroup } from "../../model/category.model";

interface Props {
  open: boolean;
  group?: CategoryGroup;
  onConfirm: (group: CategoryGroup) => void;
  onCancel: () => void;
}

export default function EditCategoryGroup(props: Props) {
  const handleCancel = () => {
    props.onCancel();
  };

  function handleConfirm() {
    props.onConfirm(group);
  };

  const [ group, setGroup ] = useState<CategoryGroup>({ name: "" });
  useEffect(() => { setGroup(props.group || { name: "", description: "" }) }, [ props ]);

  function validateForm(): boolean {
    return group.name.trim().length > 0;
  }

  return (
    <Dialog open={ props.open } onClose={ handleCancel } fullWidth>
      <DialogTitle sx={{ bgcolor: 'primary.main', color: 'white' }}>{ props.group ? "Edit Category Group" : "New Category Group" }</DialogTitle>
      <DialogContent>
        <p/>
        <TextField
          id="name" label="Group Name" autoFocus required
          margin="normal" fullWidth variant="standard"
          value={ group.name }
          onChange={(e) => setGroup({ ...group, name: e.target.value })}
        />

        <TextField
          id="name" label="Description"
          margin="normal" fullWidth variant="standard"
          value={ group.description }
          onChange={(e) => setGroup({ ...group, description: e.target.value })}
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={ handleCancel } variant="outlined">Cancel</Button>
        <Button onClick={ handleConfirm } variant="contained" disabled={ !validateForm() }>
          { props.group ? "Save" : "Add" }
        </Button>
      </DialogActions>
    </Dialog>
  );
}
