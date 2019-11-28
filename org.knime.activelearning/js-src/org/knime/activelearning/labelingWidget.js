/* global knimeTileView:false, $:false, KnimeBaseTableViewer:false*/
window.generalPurposeLabelingWidget = (function () {
    var labelingWidget = {};
    var _selectedTiles = [];
    var _initialized = false;
    var _representation,
        _value,
        _tileView,

        // function definitions
        _createContainer, _updateButtonClasses, _getHexColor, _changeSkipButton, _setupSkipButtonHandler, _labelAndLoadNext,
        _setupAddClassesButtonHandler, _createClassEditorDialog, _getRandomColor, _updateDialogClasses, _initializeView,
        _bindArrowKeys, _updateLabelClasses, _updateDropdownClasses, _filterData, _getAllPossibleValues, _invertColor,
        _hexToRgb, _selectNextTile, _getColorValue, _createRemoveDialog, _combinePossibleValues, _checkIfIsCurrentlyDisplayed,
        _changeToDefaultHeaderColor, _selectFirstTile, _combineColors;

    labelingWidget.init = function (representation, value) {
        _representation = representation;
        // Ignore previous defined row color headers and reset to default color
        _representation.table.spec.rowColorValues = _changeToDefaultHeaderColor(_representation.table.spec.rowColorValues, '#404040');
        _value = value;
        _value.possiblevalues = _combinePossibleValues(_representation, _value);
        if (Object.keys(_value.colors).length === 0) {
            _value.colors = _representation.colors;
        }
        _createContainer();
        _bindArrowKeys();
    };

    labelingWidget.validate = function () {
        return true;
    };

    labelingWidget.getComponentValue = function () {
        window.knimeTileView.getComponentValue.apply(window.knimeTileView);
        return _value;
    };

    /**
     * Changes any existing colored header rows to a default color
     */
    _changeToDefaultHeaderColor = function (rowColorValues, colorValue) {
        rowColorValues.forEach(function (part, index, data) {
            data[index] = colorValue;
        });
        return rowColorValues;
    };

    /**
     * combines possible values of representation and value
     */
    _combinePossibleValues = function (_representationn, _value) {
        var possibleValues = _representation.possiblevalues.concat(_value.possiblevalues);
        var result = possibleValues.filter(function (item, pos) {
            return possibleValues.indexOf(item) == pos;
        });
        return result;
    };

    _initializeView = function (representation, value) {
        if (_initialized === false) {
            _initialized = true;
            if (Object.keys(value.labels).length > 0) {
                var _tempSelectedTiles = value.labels;
                var _tempLength = Object.keys(_tempSelectedTiles).length - 1;
                var counter = 0;
                for (var tile in _tempSelectedTiles) {
                    var color = value.colors[_tempSelectedTiles[tile]];
                    if (!color) {
                        color = representation.colors[_tempSelectedTiles[tile]];
                    }
                    if (counter === _tempLength) {
                        _labelAndLoadNext(_hexToRgb(_getHexColor(color)), _tempSelectedTiles[tile], true, tile, true);
                    } else {
                        _labelAndLoadNext(_hexToRgb(_getHexColor(color)), _tempSelectedTiles[tile], true, tile);
                    }
                    counter++;
                }
            } else {
                if (_representation.autoSelectNextTile) {
                    _selectedTiles[_selectFirstTile().value] = true;
                }
            }
        } else {
            return;
        }
    };

    /**
     * Copy mostly copied from baseTableViewer except for two lines which set to the correct selection. This is necessary, as correct page is drawn after timeout...
     */
    window.knimeTileView._applyViewValueOld = window.knimeTileView._applyViewValue;
    window.knimeTileView._applyViewValue = function () {
        if (this._representation.enableSearching && this._value.filterString) {
            this._dataTable.search(this._value.filterString);
        }
        if (this._representation.enableColumnSearching && this._value.columnFilterStrings) {
            for (var i = 0; i < this._value.columnFilterStrings.length; i++) {
                var curValue = this._value.columnFilterStrings[i];
                if (curValue.length > 0) {
                    var column = this._dataTable.column(i);
                    $('input', column.footer()).val(curValue);
                    column.search(curValue);
                }
            }
        }
        if (this._representation.enablePaging && this._value.currentPage) {
            var self = this;
            setTimeout(function () {
                self._dataTable.page(self._value.currentPage).draw('page');
                // these two lines are new.
                _selectedTiles = self._selection;
                document.getElementById('selectedText').innerHTML = 'Selected tiles: ' + Object.keys(_selectedTiles).length;
                //
            }, 0);
        }
    };

    /**
     * Overwrite selectionChanged function to react to selection events from other views.
     */
    window.knimeTileView._selectionChangedOld = window.knimeTileView._selectionChanged;
    window.knimeTileView._selectionChanged = function (data) {
        window.knimeTileView._selectionChangedOld.apply(this, [data]);
        this._value.selection = this._selection;
        _selectedTiles = this._selection;
        for (var row in _selectedTiles) {
            if (_selectedTiles[row] === false) {
                delete _selectedTiles[row];
            }
        }
        document.getElementById('selectedText').innerHTML = 'Selected tiles: ' + Object.keys(_selectedTiles).length;
        _changeSkipButton();
    };

    /**
     * Overwrite dataTableDrawCallback function to load selection into view, if there was a previous selection
     */
    window.knimeTileView._dataTableDrawCallbackOld = window.knimeTileView._dataTableDrawCallback;
    window.knimeTileView._dataTableDrawCallback = function () {
        window.knimeTileView._dataTableDrawCallbackOld.apply(this);
        if (window.knimeTileView._dataTable !== null) {
            _initializeView(_representation, _value);
        }
    };

    /**
     * Applies the selection to the currently display page
     */
    window.knimeTileView._setSelectionOnPageOld = window.knimeTileView._setSelectionOnPage;
    window.knimeTileView._setSelectionOnPage = function () {
        window.knimeTileView._setSelectionOnPageOld.apply(this);
        var curCheckboxes = this._dataTable.column(0, {
            page: 'current'
        }).nodes().to$().children();
        for (var i = 0; i < curCheckboxes.length; i++) {
            var checkbox = curCheckboxes[i];
            checkbox.checked = this._selection[checkbox.value];
            // set tr style
            var $tr = $(checkbox).parent().parent();
            if (checkbox.checked) {
                $tr.addClass('knime-selected');
            } else {
                $tr.removeClass('knime-selected');
            }
            if ('indeterminate' in checkbox) {
                if (!checkbox.checked && this._partialSelectedRows.indexOf(checkbox.value) > -1) {
                    checkbox.indeterminate = true;
                } else {
                    checkbox.indeterminate = false;
                }
            }
            if (knimeTileView._value.labels[checkbox.value]) {
                checkbox.parentNode.parentNode.labeled = true;
            }
        }
    };

    /**
     * Overwrite Menu building function
     */
    window.knimeTileView._buildMenuOld = window.knimeTileView._buildMenu;
    window.knimeTileView._buildMenu = function () {
        window.knimeTileView._buildMenuOld.apply(this);
        var self = this;
        if (!this._representation.showUnlabeledOnly) {
            var showUnlabeledCheckbox = knimeService.createMenuCheckbox('showUnlabeledCheckbox',
                this._value.showUnlabeledOnly, function () {
                    if (this.checked) {
                        self._filterLabeldData();
                        self._value.showUnlabeledOnly = true;
                    } else {
                        self._resetFilter();
                        self._value.showUnlabeledOnly = false;
                    }
                });
            knimeService.addMenuItem('Show only unlabeled data', null, showUnlabeledCheckbox);
        }
        if (this._representation.labelcreation) {
            var addClassesButton = document.createElement('button');
            addClassesButton.type = 'button';
            addClassesButton.id = 'btnEditClasses';
            addClassesButton.innerHTML = '&#x1F527;';
            addClassesButton.onclick = function (e) {
                document.getElementById('dlgEdit').showModal();
            };
            knimeService.addMenuItem('Create custom classes', null, addClassesButton);
        }
        if (!this._representation.autoSelectNext) {
            var autoSelectCheckbox = knimeService.createMenuCheckbox('autoSelectCheckbox',
                this._value.autoSelectNextTile, function () {
                    if (this.checked) {
                        self._value.autoSelectNextTile = true;
                    } else {
                        self._value.autoSelectNextTile = false;
                    }
                });
            knimeService.addMenuItem('Auto select next tile when labeled', null, autoSelectCheckbox);
        }
    };

    /**
     * Overwrite click selection behavior
     */
    window.knimeTileView._setSelectionHandlers = function () {
        KnimeBaseTableViewer.prototype._setSelectionHandlers.apply(this);
        if (!this._representation.enableSelection) {
            return;
        }
        this._getJQueryTable().find('tbody').addClass('knime-selection-enabled').on('click', 'tr', function (e) {
            var index;
            if (e.target && e.target.tagName === 'INPUT' && e.target.type === 'checkbox') {
                if (e.target.checked) {
                    _selectedTiles[e.target.value] = true;
                    document.getElementById('selectedText').innerHTML = 'Selected tiles: ' + Object.keys(_selectedTiles).length;
                    _changeSkipButton();
                } else {
                    if (_selectedTiles[e.target.value]) {
                        delete _selectedTiles[e.target.value];
                        document.getElementById('selectedText').innerHTML = 'Selected tiles: ' + Object.keys(_selectedTiles).length;
                    }
                    _changeSkipButton();
                }
                return;
            } else {
                $(e.currentTarget).find('input[type="checkbox"]').click();
            }
        });
    };

    _createContainer = function () {
        var labelingContainer = document.createElement('div');
        labelingContainer.id = 'labelingContainer';
        document.body.appendChild(labelingContainer);

        _representation.containerElement = labelingContainer;
        _tileView = window.knimeTileView;
        _tileView.init(_representation, _value);

        if (_representation.useProgressBar) {
            var progressText = document.createElement('div');
            progressText.id = 'progressText';

            var currentProgress = document.createElement('div');
            currentProgress.id = 'labCurrentProgress';

            var currentProgressbar = document.createElement('div');
            currentProgressbar.id = 'labCurrentProgressBar';

            if (!_representation.title || _representation.title === '') {
                currentProgress.className = 'labCurrentProgressWithoutTitle';
            }

            currentProgress.appendChild(currentProgressbar);
            var dataTableWrapper = document.getElementsByClassName('dataTables_wrapper')[0];
            dataTableWrapper.parentNode.insertBefore(currentProgress, dataTableWrapper);
            dataTableWrapper.parentNode.insertBefore(progressText, dataTableWrapper);
            // Initialize progress text
            document.getElementById('progressText').innerHTML = _filterData(_getAllPossibleValues()).length + ' / ' +
                _tileView._representation.table.rows.length + ' labeled';
        }

        var labelingButtonGroup = document.createElement('div');
        labelingButtonGroup.id = 'labelingButtonGroup';
        labelingButtonGroup.className = 'row';
        labelingContainer.appendChild(labelingButtonGroup);

        var labelingText = document.createElement('span');
        labelingText.textContent = 'Label as:';
        labelingButtonGroup.appendChild(labelingText);

        var selectedText = document.createElement('span');
        selectedText.id = 'selectedText';
        selectedText.textContent = 'Selected Tiles: 0';
        labelingButtonGroup.appendChild(selectedText);

        var editDialog = _createClassEditorDialog();
        labelingButtonGroup.appendChild(editDialog);
        var removeDialog = _createRemoveDialog();
        editDialog.appendChild(removeDialog);


        var labelingDoneText = document.createElement('h1');
        labelingDoneText.innerHTML = 'All elements have been labeled.';
        labelingDoneText.style = 'text-align: center; margin-top: 30px;';

        var labelingButtons = document.createElement('div');
        labelingButtons.id = 'divLabels';
        labelingButtons.className = 'row';
        labelingButtons.style = 'margin-left:-5px';
        labelingButtonGroup.appendChild(labelingButtons);

        var skipButtonContainer = document.createElement('div');
        skipButtonContainer.id = 'skipButtonContainer';
        var skipButton = document.createElement('button');
        skipButton.type = 'button';
        skipButton.id = 'btnSkip';
        skipButton.value = 'Skip';
        skipButton.innerHTML = 'Skip';
        skipButton.className = 'knime-qf-button btnLabel';
        skipButton.style.visibility = 'hidden';
        skipButtonContainer.appendChild(skipButton);
        labelingButtonGroup.appendChild(skipButtonContainer);
        _updateLabelClasses();
        _setupSkipButtonHandler(skipButton);
    };

    _setupSkipButtonHandler = function (skipButton) {
        skipButton.onclick = function (e) {
            if (e.target.innerHTML === 'Skip') {
                _labelAndLoadNext(_hexToRgb('#F0F0F0'), e.target.innerHTML);
            } else {
                for (var selectedTile in _selectedTiles) {
                    if (_value.labels[selectedTile]) {
                        delete _value.labels[selectedTile];
                    }
                }
                _labelAndLoadNext(_hexToRgb('#404040'), '');
            }
        };
    };

    _bindArrowKeys = function () {
        var leftArrowKeyCode = 37;
        var rightArrowKeyCode = 39;
        document.onkeydown = function (e) {
            switch (e.keyCode) {
                case leftArrowKeyCode:
                    _tileView._getJQueryTable().DataTable().page('previous').draw('page');
                    break;
                case rightArrowKeyCode:
                    _tileView._getJQueryTable().DataTable().page('next').draw('page');
                    break;
            }
        };
    };

    // As postet at https://stackoverflow.com/questions/3942878/how-to-decide-font-color-in-white-or-black-depending-on-background-color/3943023#3943023
    // this should increase the readability of the text color.
    // --------------------------------------------------------------------------------------------------------------
    _invertColor = function (rgb, bw) {
        var r = rgb.r,
            g = rgb.g,
            b = rgb.b;
        if (bw) {
            return (r * 0.299 + g * 0.587 + b * 0.114) > 186
                ? { r: 0, g: 0, b: 0 }
                : { r: 255, g: 255, b: 255 };
        }
        // invert color components
        r = (255 - r).toString(16);
        g = (255 - g).toString(16);
        b = (255 - b).toString(16);
        return { r: r, g: g, b: b };
    };
    // --------------------------------------------------------------------------------------------------------------

    window.knimeTileView._filterLabeldData = function () {
        var concatValues = _getAllPossibleValues();
        _tileView._getJQueryTable().DataTable().column(1).search('^(?!.*(' + concatValues + ')).*', true, false).draw();
    };

    window.knimeTileView._resetFilter = function () {
        _tileView._getJQueryTable().DataTable().columns().search('').draw();
    };

    _createRemoveDialog = function () {
        var removeDialog = document.createElement('dialog');
        removeDialog.id = 'dlgRemove';

        var removeDialogText = document.createElement('div');
        removeDialogText.innerHTML = '';
        removeDialogText.id = 'dlgRemoveText';
        removeDialog.appendChild(removeDialogText);


        var removeDialogButtonKeep = document.createElement('button');
        removeDialogButtonKeep.innerHTML = 'Keep';
        removeDialogButtonKeep.onclick = function () {
            document.getElementById('dlgRemove').close();
        };
        removeDialog.appendChild(removeDialogButtonKeep);

        var removeDialogButtonAccept = document.createElement('button');
        removeDialogButtonAccept.innerHTML = 'Remove';
        removeDialogButtonAccept.id = 'btnRemove';
        removeDialogButtonAccept.onclick = function () {
            var select = document.getElementById('slcClassesEdit');
            var classes = [...select.options].filter(option => option.selected).map(option => option.value);
            for (var i = 0; i < classes.length; i++) {
                var index = _value.possiblevalues.indexOf(classes[i]);
                if (index > -1) {
                    _value.possiblevalues.splice(index, 1);
                }
            }
            _tileView._getJQueryTable().DataTable().clear().draw();
            var initialChunkSize = 100;
            _tileView._addDataToTable(_tileView._representation.initialPageSize, initialChunkSize);
            for (var k = Object.values(_value.labels).length; k > 0; k--) {
                for (var j = classes.length; j > 0; j--) {
                    if (Object.values(_value.labels)[k - 1] === classes[j - 1]) {
                        delete _value.labels[Object.keys(_value.labels)[k - 1]];
                    }
                }
            }
            _tileView._value.currentPage = _tileView._value.tablesettings.currentPage;
            _initialized = false;
            _initializeView(_tileView._representation, _tileView._value);
            _updateLabelClasses();
            document.getElementById('btnRemoveLabelClass').hidden = true;
            document.getElementById('dlgRemove').close();
        };
        removeDialog.appendChild(removeDialogButtonAccept);

        return removeDialog;
    };

    /**
     * Create label class editor dialog
     */
    _createClassEditorDialog = function () {
        var editDialog = document.createElement('dialog');
        editDialog.id = 'dlgEdit';

        var editHeader = document.createElement('h1');
        editHeader.innerHTML = 'Labels';
        editDialog.appendChild(editHeader);

        var editTextInput = document.createElement('input');
        editTextInput.id = 'tbxNewLabel';
        editTextInput.style = 'width: 50%;';
        editTextInput.type = 'text';
        editDialog.appendChild(editTextInput);

        var editAddButton = document.createElement('button');
        editAddButton.id = 'btnAddNewLabelClass';
        editAddButton.style = 'width: 50%;';
        editAddButton.type = 'button';
        editAddButton.innerHTML = 'Add';
        _setupAddClassesButtonHandler(editAddButton);
        editDialog.appendChild(editAddButton);
        editDialog.appendChild(document.createElement('br'));
        var editList = document.createElement('select');
        editList.id = 'slcClassesEdit';
        editList.style = 'width: 100%;';
        editList.multiple = true;
        editDialog.appendChild(editList);

        editList.onchange = function (e) {
            var select = document.getElementById('slcClassesEdit');
            var classes = [...select.options].filter(option => option.selected);
            if (classes.length > 0) {
                document.getElementById('btnRemoveLabelClass').hidden = false;
            }
        };

        editDialog.appendChild(document.createElement('br'));

        var editInfoText = document.createElement('p');
        editInfoText.innerHTML = 'Labels set in the node dialog cannot be removed';
        editInfoText.style = 'color:grey';
        editDialog.appendChild(editInfoText);

        var removeButton = document.createElement('button');
        removeButton.id = 'btnRemoveLabelClass';
        removeButton.style = 'width: 100%;';
        removeButton.type = 'button';
        removeButton.innerHTML = 'Remove';
        removeButton.hidden = true;

        removeButton.onclick = function (e) {
            var select = document.getElementById('slcClassesEdit');
            var classes = [...select.options].filter(option => option.selected).map(option => option.value);
            var removeDialogText = document.getElementById('dlgRemoveText');
            removeDialogText.innerHTML = 'Are you sure you want to delete the selected classes? There are ' + _filterData(classes.join('|')).length + ' tiles marked.';

            document.getElementById('dlgRemove').showModal();

        };

        editDialog.appendChild(removeButton);
        editDialog.appendChild(document.createElement('br'));

        var closeButton = document.createElement('button');
        closeButton.id = 'btnCloseDialog';
        closeButton.style = 'float: right;';
        closeButton.type = 'button';
        closeButton.innerHTML = 'Close';
        closeButton.onclick = function (e) {
            document.getElementById('dlgEdit').close();
            _updateLabelClasses();
        };
        editDialog.appendChild(closeButton);

        return editDialog;
    };

    _setupAddClassesButtonHandler = function (addClassesButton) {
        addClassesButton.onclick = function (e) {
            var value = document.getElementById('tbxNewLabel').value;
            var oldIndex = _representation.possiblevalues.indexOf(value);
            var newIndex = _value.possiblevalues.indexOf(value);
            if (oldIndex === -1 && newIndex === -1 && value !== '') {
                _value.possiblevalues.push(value);
                document.getElementById('tbxNewLabel').value = '';
                _updateLabelClasses();
            } else {
                alert('Label could not be added');
            }
        };
    };

    /**
     * Updates all label classes
     */
    _updateLabelClasses = function () {
        _updateDialogClasses();

        if (_value.possiblevalues.length < 8) {
            _updateButtonClasses();
        } else {
            _updateDropdownClasses();
        }
    };

    /**
     * Updates the dropdown label classes
     */
    _updateDropdownClasses = function () {
        var buttons = document.getElementById('divLabels');
        buttons.innerHTML = '';
        _updateButtonClasses(7);
        var counter = 7;

        var dropdownContainer = document.createElement('span');
        dropdownContainer.id = 'dropdownContainer';
        var selectClasses = document.createElement('select');
        selectClasses.id = 'slcClassesDropdown';
        dropdownContainer.appendChild(selectClasses);
        buttons.appendChild(dropdownContainer);

        for (var k = counter; k < _value.possiblevalues.length; k++) {
            var addedLabelOption = document.createElement('option');
            addedLabelOption.className = 'btnLabel';
            addedLabelOption.value = _value.possiblevalues[k];
            addedLabelOption.innerHTML = _value.possiblevalues[k];
            _getColorValue(k);
            selectClasses.appendChild(addedLabelOption);
        }

        var labelButton = document.createElement('button');
        labelButton.style = 'margin:5px';
        labelButton.type = 'button';
        labelButton.className = 'btnLabel';
        labelButton.value = 'dropDownLabel';
        labelButton.innerHTML = 'Label';
        labelButton.id = 'labelButton';
        labelButton.onclick = function (e) {
            var dropdown = document.getElementById('slcClassesDropdown');
            var value = dropdown.options[dropdown.selectedIndex].value;
            var color = knimeTileView._representation.colors[value];
            if (!color) {
                color = knimeTileView._value.colors[value];
            }
            _labelAndLoadNext(_hexToRgb(_getHexColor(color)), value);
        };
        dropdownContainer.appendChild(labelButton);
    };

    /**
     * Updates the dialog label classes
     */
    _updateDialogClasses = function () {
        var options = document.getElementById('slcClassesEdit');

        options.innerHTML = '';

        for (var j = 0; j < _representation.possiblevalues.length; j++) {
            var labelOption = document.createElement('option');
            labelOption.disabled = true;
            labelOption.value = _representation.possiblevalues[j];
            labelOption.innerHTML = _representation.possiblevalues[j];
            options.appendChild(labelOption);
        }

        for (var k = _representation.possiblevalues.length; k < _value.possiblevalues.length; k++) {
            var addedLabelOption = document.createElement('option');
            addedLabelOption.disabled = false;
            addedLabelOption.value = _value.possiblevalues[k];
            addedLabelOption.innerHTML = _value.possiblevalues[k];
            options.appendChild(addedLabelOption);
        }
    };

    /**
     * Updates the button label classes
     */
    _updateButtonClasses = function (maxAmount) {
        var buttons = document.getElementById('divLabels');
        buttons.innerHTML = '';
        var textColor, bgColor;
        var counter = 0;
        var maxStringLength = 15;

        for (var k = 0; k < _value.possiblevalues.length; k++) {
            if (counter === maxAmount) {
                break;
            }
            var addedLabelButton = document.createElement('button');
            addedLabelButton.style = 'margin:5px';
            addedLabelButton.style.setProperty('word-break', 'break-all', '');
            addedLabelButton.type = 'button';
            addedLabelButton.className = 'btnLabel';
            addedLabelButton.value = _value.possiblevalues[k];
            if (_value.possiblevalues[k].length < maxStringLength) {
                addedLabelButton.innerHTML = _value.possiblevalues[k];
            } else {
                addedLabelButton.title = _value.possiblevalues[k];
                addedLabelButton.completeName = _value.possiblevalues[k];
                addedLabelButton.innerHTML = _value.possiblevalues[k].substr(0, maxStringLength - 1) + '...';

            }
            bgColor = _getColorValue(k);
            addedLabelButton.style.background = _getHexColor(bgColor);

            textColor = _invertColor(_hexToRgb(_getHexColor(bgColor)), true);
            addedLabelButton.style.color = 'rgb(' + textColor.r + ',' + textColor.g + ', ' + textColor.b + ')';
            addedLabelButton.onclick = function (e) {
                if (e.target.completeName) {
                    _labelAndLoadNext(e.target.style.backgroundColor, e.target.completeName);
                } else {
                    _labelAndLoadNext(e.target.style.backgroundColor, e.target.innerHTML);
                }
            };
            buttons.appendChild(addedLabelButton);
            counter++;
        }
    };

    _getColorValue = function (index) {

        var bgColor;
        var defaultColor = 15790320;
        if (_representation.colorscheme === 'None') {
            bgColor = defaultColor;
        } else if (_value.possiblevalues[index] in _value.colors) {
            bgColor = _value.colors[_value.possiblevalues[index]];
            // if dropdown is used, then colors should all be the same (6 is dropdown limit at the moment)
            // TODO make dropdown limit be configurable
        } else if (index > 6) {
            bgColor = defaultColor;
            _value.colors[_value.possiblevalues[index]] = bgColor;
        } else {
            bgColor = _getRandomColor();
            _value.colors[_value.possiblevalues[index]] = bgColor;
        }
        return bgColor;
    };

    _getRandomColor = function () {
        var maxHexNumber = 16777215;
        var color = Math.floor(Math.random() * maxHexNumber);
        return color;
    };

    _checkIfIsCurrentlyDisplayed = function (rowId) {
        var currentCheckboxes = knimeTileView._curCells[0].parentElement.parentElement.getElementsByClassName('selection-cell');
        var displayedCheckbox = null;
        for (var checkbox in currentCheckboxes) {
            if (checkbox === 'length') {
                break;
            }
            if (currentCheckboxes[checkbox].children[0].value === rowId) {
                displayedCheckbox = currentCheckboxes[checkbox];
                break;
            }
        }
        return displayedCheckbox;
    };


    _labelAndLoadNext = function (labelColor, labelText, justLabel, rowName, lastToLabel) {
        // only work if something is selected. TODO disable buttons.
        if (Object.keys(_selectedTiles).length > 0 || justLabel) {
            var selectedRows = [];
            if (justLabel) {
                selectedRows[0] = rowName;
            } else {
                for (var i = Object.keys(_selectedTiles).length - 1; i >= 0; i--) {
                    rowName = Object.keys(_selectedTiles)[i];
                    _tileView._selection[rowName] = false;
                    // Remove operation
                    var currentTile = _checkIfIsCurrentlyDisplayed(rowName);
                    if (currentTile !== null) {
                        if (labelText === '') {
                            // click the selected Tile
                            currentTile.parentNode.parentNode.labeled = false;
                            $(currentTile.children[0]).click();
                        } else {
                            currentTile.parentNode.parentNode.labeled = true;
                            $(currentTile.children[0]).click();
                        }
                    }
                    selectedRows.push(rowName);
                    delete _selectedTiles[rowName];
                }
            }
            var rgbObject, row;

            // Transform from object to rgb string
            if (typeof labelColor === 'object') {
                rgbObject = labelColor;
                labelColor = 'rgb(' + rgbObject.r + ',' + rgbObject.g + ',' + rgbObject.b + ')';
            } else {
                var rgbSplit = labelColor.split('(')[1].substring(0, labelColor.split('(')[1].length - 1).split(',');
                rgbObject = { r: parseInt(rgbSplit[0], 10), g: parseInt(rgbSplit[1], 10), b: parseInt(rgbSplit[2], 10) };
            }
            var invertedColor = _invertColor(rgbObject, true);
            var invertedRGBColor = 'rgb(' + invertedColor.r + ',' + invertedColor.g + ',' + invertedColor.b + ')';
            for (row in selectedRows) {
                var rowNumber;
                for (var j = 0; j < _tileView._dataTable.data().length; j++) {
                    if (justLabel) {
                        if (_tileView._dataTable.data()[j][0] === selectedRows[row]) {
                            rowNumber = j;
                            break;
                        }
                    } else if (_tileView._dataTable.data()[j][0] === selectedRows[row]) {
                        rowNumber = j;
                        break;
                    }
                }
                // Check for hex string
                _tileView._dataTable.data()[rowNumber][1] = _tileView._dataTable.data()[rowNumber][1].replace(
                    /background-color:\s*#[A-Fa-f0-9]{6};*/, 'background-color: ' + labelColor + ';');
                // Check for rgb background string
                _tileView._dataTable.data()[rowNumber][1] = _tileView._dataTable.data()[rowNumber][1].replace(
                    /background-color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/, 'background-color: ' + labelColor + ';');
                // Check if text color value is set and if yes replace it
                if (/"color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/.test(_tileView._dataTable.data()[rowNumber][1])) {
                    _tileView._dataTable.data()[rowNumber][1] = _tileView._dataTable.data()[rowNumber][1].replace(
                        /"color:\s*rgb\(\d*,\s*\d*,\s*\d*\);*/, '"color: ' + invertedRGBColor + ';');
                } else {
                    _tileView._dataTable.data()[rowNumber][1] = _tileView._dataTable.data()[rowNumber][1].replace(
                        /style="/, 'style="color: ' + invertedRGBColor + ';');
                }
                // Replace the text of the tile to the new label
                _tileView._dataTable.data()[rowNumber][1] = _tileView._dataTable.data()[rowNumber][1].replace(
                    />.*<\/div>/, '>' + labelText + '</div>');
                _tileView._dataTable.row(rowNumber).invalidate();
                // Save the current label into the view value, if it is not empty.
                if (labelText !== '') {
                    _value.labels[_representation.table.rows[rowNumber].rowKey] = labelText;
                }
            }
            _value.selection = [];
            if (_value.showUnlabeledOnly) {
                _tileView._filterLabeldData();
            }
            // Calculate the percentage of already labeled tiles and display it
            var progress = 1 - (_tileView._representation.table.rows.length - _filterData(_getAllPossibleValues()).length) / _tileView._representation.table.rows.length;
            if (_tileView._representation.useProgressBar) {
                document.getElementById('labCurrentProgressBar').style.width = progress * 100 + '%';
                document.getElementById('progressText').innerHTML = _filterData(_getAllPossibleValues()).length + ' / ' + _tileView._representation.table.rows.length + ' labeled';
            }
            if (_tileView._value.autoSelectNextTile && labelText !== '') {
                var selectedRowName;
                if (justLabel && lastToLabel) {
                    // nothing todo as no tile should be selected
                } else if (!justLabel) {
                    knimeService.setSelectedRows(_representation.table.id, [], false);
                    selectedRowName = _selectNextTile(selectedRows);
                    _selectedTiles[selectedRowName] = true;
                }
            }
            // check for amount of selected tiles
            if (Object.keys(_selectedTiles).length) {
                document.getElementById('selectedText').innerHTML = 'Selected tiles: ' + Object.keys(_selectedTiles).length;
            } else {
                // set to 0 if selected is undefined (results from fresh loading)
                document.getElementById('selectedText').innerHTML = 'Selected tiles: ' + 0;
            }
        }
    };

    _selectNextTile = function (selectedRows) {
        var foundNext = false;
        var currentCheckboxes = knimeTileView._curCells[0].parentElement.parentElement.getElementsByClassName('selection-cell');
        var lastCheckbox = 0;
        var row, checkbox;
        for (row in selectedRows) {
            var tempRowNumber = selectedRows[row];
            var newRowId = _tileView._dataTable.rows().eq(0).filter(function (rowIdx) {
                return _tileView._dataTable.cell(rowIdx, 0).data() === tempRowNumber ? true : false;
            });
            newRowId = newRowId[0];
            if (lastCheckbox < newRowId) {
                lastCheckbox = newRowId;
            }
        }

        for (checkbox in currentCheckboxes) {
            if (checkbox === 'length') {
                break;
            }
            var checkboxIndex = _tileView._dataTable.row(currentCheckboxes[checkbox]).index();
            if (lastCheckbox < checkboxIndex) {
                $(currentCheckboxes[checkbox]).click();
                foundNext = true;
                return currentCheckboxes[checkbox].children[0].value;
            }
        }
        if (!foundNext) {
            var lastRow = currentCheckboxes[currentCheckboxes.length - 1];
            _tileView._getJQueryTable().DataTable().page('next').draw('page');
            return _selectFirstTile().value;
        }
        return null;
    };

    _selectFirstTile = function () {
        var currentCheckboxes = knimeTileView._curCells[0].parentElement.parentElement.getElementsByClassName('selection-cell');
        $(currentCheckboxes[0]).click();
        return currentCheckboxes[0].children[0];
    };

    _getAllPossibleValues = function () {
        var concatValues = 'Skip|';
        var possibleValue, possibleNewValues;
        for (possibleValue in _tileView._representation.possiblevalues) {
            concatValues += _representation.possiblevalues[possibleValue] + '|';
        }
        concatValues = concatValues.slice(0, -1);
        if (_tileView._value.possiblevalues.length > 0) {
            for (possibleNewValues in _tileView._value.possiblevalues) {
                concatValues += '|' + _value.possiblevalues[possibleNewValues];
            }
            concatValues = concatValues.slice(0, concatValues.length - 1);
        }
        return concatValues;
    };

    _filterData = function (searchTerm) {
        var searchTerms = searchTerm.split('|');
        return _tileView._getJQueryTable().DataTable().column(1).data().filter(function (value, index) {
            var containsSearchTerm = false;
            for (var searchTerm in searchTerms) {
                if (value.includes(searchTerms[searchTerm])) {
                    containsSearchTerm = true;
                }
            }
            return containsSearchTerm;
        });
    };

    _changeSkipButton = function (changeToRemoveLabel) {
        var skipButton = document.getElementById('btnSkip');
        if (Object.keys(_selectedTiles).length > 0) {
            skipButton.style.visibility = 'visible';
        } else {
            skipButton.style.visibility = 'hidden';
        }

        var onlyUnlabeled = true;
        for (var selectedTile in _selectedTiles) {
            if (knimeTileView._value.labels[selectedTile]) {
                onlyUnlabeled = false;
            }
        }
        if (onlyUnlabeled) {
            skipButton.innerHTML = 'Skip';
        } else {
            skipButton.innerHTML = 'Remove Label';
        }
    };

    /**
     * Return the hex representation of the input integer
     */
    _getHexColor = function (number) {
        var hexCode = ((number) >>> 0).toString(16).slice(-6);
        while (hexCode.length < 6) {
            hexCode = '0' + hexCode;
        }
        hexCode = '#' + hexCode;
        return hexCode;
    };

    _hexToRgb = function (hex) {
        var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return result ? {
            r: parseInt(result[1], 16),
            g: parseInt(result[2], 16),
            b: parseInt(result[3], 16)
        } : null;
    };

    return labelingWidget;
})();
