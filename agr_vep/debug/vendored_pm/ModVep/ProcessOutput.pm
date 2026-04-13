package ModVep::ProcessOutput;

use strict;
use warnings;

use File::Copy;
use Path::Class;
use Try::Tiny;

use base ('ModVep::BaseModVep');


sub run {
    my $self = shift;

    my $input_file = $self->required_param('vep_input_file') . '.vep.vcf';
    my $output_file = $input_file . '.processed';
    unlink $output_file if -e $output_file;

    my $reverse_map = $self->get_reverse_chromosome_map($self->required_param('refseq_chromosomes'),
							$self->required_param('mod'));

    my ($nr_csq_cols, $allele_ix, $pick_flag_ix, $consequence_ix);
    my $in_fh = file($input_file)->openr;
    my $out_fh = file($output_file)->openw;
    
    $self->param('process_failure', 0);
    try{
	while (my $line = $in_fh->getline) {
	    chomp $line;
	    if ($line =~ /^#/) {
		if ($line =~ /^##INFO=<ID=CSQ.+Format:\s(.+)>$/) {
		    my @csq_cols = split(/\|/, $1);
		    $nr_csq_cols = scalar(@csq_cols);
		    ($allele_ix) = grep {$csq_cols[$_] eq 'Allele'} (0 .. $nr_csq_cols - 1);
		    ($pick_flag_ix) = grep {$csq_cols[$_] eq 'PICK'} (0 .. $nr_csq_cols - 1);
		    ($consequence_ix) = grep {$csq_cols[$_] eq 'Consequence'} (0 .. $nr_csq_cols - 1);
		    $line =~ s/\|PICK\|/\|Gene_level_consequence\|/;
		}
		$out_fh->print("$line\n") if $input_file =~ /_0+1\.vep\.vcf$/;
		next;
	    }
	    my @columns = split("\t", $line);
	    $columns[0] = $reverse_map->{$columns[0]};
	    
	    my ($pre_csq, $csq_string, $post_csq) = $columns[7] =~ /^(.*CSQ=)([^;]+)(.*)$/;
	    die "Could not parse line for CSQ:\n$line\n" unless defined $pre_csq and defined $csq_string and defined $post_csq;
	    my @csq_entries = split(',', $csq_string);
	    my %most_severe_consequences;
	    my @csq_splits;
	    for my $csq_entry(@csq_entries) {
		my @csq_cols = split(/\|/, $csq_entry, -1);
		die "Mismatch in nr. of CSQ fields found - expecting ${nr_csq_cols}, found " . scalar @csq_cols . "\n$line\n"
		    unless @csq_cols == $nr_csq_cols;

		push @csq_splits, \@csq_cols;
		if ($csq_cols[$pick_flag_ix]) {
		    $most_severe_consequences{$csq_cols[$allele_ix]} = $csq_cols[$consequence_ix];
		}
	    }
	    my @new_csq_entries;
	    for my $csq_entry_split(@csq_splits) {
		$csq_entry_split->[$pick_flag_ix] = $most_severe_consequences{$csq_entry_split->[$allele_ix]};
		push @new_csq_entries, join('|', @$csq_entry_split);
	    }
	    $columns[7] = $pre_csq . join(',', @new_csq_entries) . $post_csq;
	    $out_fh->print(join("\t", @columns) . "\n");
	}
    }
    catch {
	$self->param('process_failure', 1);
	$self->warning($_);
    };

    unlink $input_file unless $self->param('process_failure');
}


sub write_output {
    my $self = shift;

    if ($self->param('process_failure')) {
	$self->dataflow_output_id([{vep_input_file => $self->param('vep_input_file')}], 2);
    }
}

    
1;
